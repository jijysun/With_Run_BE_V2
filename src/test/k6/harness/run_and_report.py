#!/usr/bin/env python3
"""
k6 부하테스트 실행 + 결과 비교 자동화 하네스.

지금까지(2026-07 초) 매 회차마다 사람이 손으로 했던 작업 —
  k6 실행 -> result/*.json 확인 -> 직전 회차와 지표 비교 -> analysis/*.md에 표+해석 기록
을 스크립트 하나로 묶는다. 로컬/원격(EC2 배포 후) 어디를 타겟으로 하든 동일하게 쓸 수 있도록
WS_URL을 그대로 노출한다(WithRun_Chat_LoadTest_*.js가 이미 이 env var를 지원).

사용 예:
  # 로컬 200VU 실행 + 직전 회차와 자동 비교 + analysis 문서에 append
  python src/test/k6/harness/run_and_report.py run \\
      --script src/test/k6/With_Run_Chat_LoadTest_V2.js \\
      --jwt-secret <JWT_SECRET_KEY> --vu 200 --duration 180 --label 200vu

  # 나중에 원격 배포 타겟으로 재실행할 때는 --ws-url만 바꾸면 됨
  python src/test/k6/harness/run_and_report.py run \\
      --script src/test/k6/With_Run_Chat_LoadTest_V2.js \\
      --jwt-secret <...> --vu 200 --label 200vu-ec2 \\
      --ws-url ws://<EC2_HOST>:8080/api/ws --mapping-file ./result/loadtest-mapping-200.json

  # 이미 나온 결과 파일끼리 나중에 다시 비교만 하고 싶을 때
  python src/test/k6/harness/run_and_report.py report --json src/test/k6/result/20260711-1728-200vu.json
"""
import argparse
import json
import re
import subprocess
import sys
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path

# Windows 콘솔 기본 코드페이지(cp949)로는 한글 분석 문서에 쓰는 이모지/특수문자(—, ⚠️ 등)를
# 못 그려서 죽는다 — stdout/stderr을 UTF-8로 강제한다.
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")

REPO_ROOT = Path(__file__).resolve().parents[4]
RESULT_DIR = Path(__file__).resolve().parent.parent / "result"
ANALYSIS_DIR = Path(__file__).resolve().parent.parent / "analysis"
ANALYSIS_FILE = ANALYSIS_DIR / "부하테스트_분석.md"

# result/{YYYYMMDD-HHmm}-{label}.json 형식만 이 하네스가 다룬다(With_Run_Chat_LoadTest_*.js의 handleSummary와 일치).
FILENAME_RE = re.compile(r"^(?P<ts>\d{8}-\d{4})-(?P<label>.+)\.json$")


@dataclass
class RunResult:
    path: Path
    timestamp: datetime
    label: str
    data: dict


def parse_result_filename(path: Path) -> tuple[datetime, str] | None:
    m = FILENAME_RE.match(path.name)
    if not m:
        return None
    return datetime.strptime(m.group("ts"), "%Y%m%d-%H%M"), m.group("label")


def load_result(path: Path) -> RunResult:
    parsed = parse_result_filename(path)
    if parsed is None:
        raise ValueError(f"파일명이 {{timestamp}}-{{label}}.json 형식이 아님: {path.name}")
    ts, label = parsed
    with path.open(encoding="utf-8") as f:
        data = json.load(f)
    return RunResult(path=path, timestamp=ts, label=label, data=data)


def find_previous_result(current: Path, compare_to: Path | None) -> RunResult | None:
    """비교 대상을 명시하지 않으면 현재 파일보다 시간상 바로 이전 회차를 자동으로 고른다."""
    if compare_to is not None:
        return load_result(compare_to)

    candidates = []
    for p in RESULT_DIR.glob("*.json"):
        if p.resolve() == current.resolve():
            continue
        parsed = parse_result_filename(p)
        if parsed is None:
            continue
        candidates.append((parsed[0], p))

    if not candidates:
        return None

    candidates.sort(key=lambda t: t[0])
    current_ts = parse_result_filename(current)[0]
    prior = [c for c in candidates if c[0] < current_ts]
    if not prior:
        return None
    return load_result(prior[-1][1])


# ===== k6 커스텀 메트릭 -> 사람이 읽는 지표로 추출 =====
# k6는 값이 0이면 요약 JSON에서 해당 커스텀 메트릭 자체를 생략하기도 하므로 항상 기본값을 둔다.

def metric_values(data: dict, name: str) -> dict:
    return data.get("metrics", {}).get(name, {}).get("values", {})


def extract_metrics(data: dict) -> dict:
    latency = metric_values(data, "chat_broadcast_latency_ms")
    sent = metric_values(data, "chat_messages_sent")
    received = metric_values(data, "chat_messages_received")
    connect_rate = metric_values(data, "chat_connect_success_rate")
    connect_errors = metric_values(data, "chat_connect_errors")
    send_errors = metric_values(data, "chat_send_errors")
    ws_connecting = metric_values(data, "ws_connecting")

    return {
        "latency_avg": latency.get("avg"),
        "latency_p90": latency.get("p(90)"),
        "latency_p95": latency.get("p(95)"),
        "latency_max": latency.get("max"),
        "sent_rate": sent.get("rate"),
        "sent_count": sent.get("count"),
        "received_rate": received.get("rate"),
        "received_count": received.get("count"),
        "connect_success_rate": connect_rate.get("rate"),
        "connect_errors": connect_errors.get("count", 0),
        "send_errors": send_errors.get("count", 0),
        "ws_connecting_avg": ws_connecting.get("avg"),
        "ws_connecting_p95": ws_connecting.get("p(95)"),
    }


def ratio(curr: float | None, prev: float | None) -> str:
    if curr is None or prev is None or prev == 0:
        return "N/A"
    return f"{curr / prev:.2f}배"


def fmt(v, unit=""):
    if v is None:
        return "N/A"
    if isinstance(v, float):
        return f"{v:.2f}{unit}"
    return f"{v}{unit}"


# ===== 리포트(마크다운) 생성 =====

def render_report(curr: RunResult, prev: RunResult | None) -> str:
    cm = extract_metrics(curr.data)
    lines = []
    lines.append(f"## {curr.timestamp:%Y-%m-%d %H:%M} · {curr.label}\n")

    if prev is None:
        lines.append("_비교 대상 회차 없음(첫 실행이거나 이전 결과가 result/에 없음)_\n")
        pm = {}
    else:
        pm = extract_metrics(prev.data)
        lines.append(f"베이스라인: `{prev.path.name}` ({prev.timestamp:%Y-%m-%d %H:%M} · {prev.label})\n")

    header = "| 지표 | 이번 회차 | 이전 회차 | 배수 |\n|---|---|---|---|\n"
    rows = [
        ("발신 처리량 (msg/s)", "sent_rate"),
        ("수신(팬아웃) 처리량 (msg/s)", "received_rate"),
        ("지연 avg (ms)", "latency_avg"),
        ("지연 p90 (ms)", "latency_p90"),
        ("지연 p95 (ms)", "latency_p95"),
        ("지연 max (ms)", "latency_max"),
        ("연결 수립 avg (ms)", "ws_connecting_avg"),
        ("연결 수립 p95 (ms)", "ws_connecting_p95"),
    ]
    table = header
    for label, key in rows:
        table += f"| {label} | {fmt(cm.get(key))} | {fmt(pm.get(key))} | {ratio(cm.get(key), pm.get(key))} |\n"
    lines.append(table)

    rate = cm.get("connect_success_rate")
    rate_str = f"{rate * 100:.1f}%" if rate is not None else "N/A"
    lines.append(
        f"\n연결 성공률: {rate_str} "
        f"(connect 에러 {cm.get('connect_errors', 0)}건, send 에러 {cm.get('send_errors', 0)}건)\n"
    )

    # 자동 이상 신호 탐지: max 지연시간이 avg 지연시간 대비 불균형하게 증가했는지
    # (2026-07-11 200VU 회차에서 사람이 손으로 짚었던 패턴 — avg/p95는 부하량과 비례했는데 max만 6.6배 튐)
    def safe_ratio(curr_v, prev_v):
        if not prev_v or curr_v is None:
            return None
        return curr_v / prev_v

    avg_ratio = safe_ratio(cm.get("latency_avg"), pm.get("latency_avg"))
    max_ratio = safe_ratio(cm.get("latency_max"), pm.get("latency_max"))
    if avg_ratio and max_ratio and max_ratio > avg_ratio * 1.5:
        lines.append(
            f"\n⚠️ **자동 플래그**: max 지연시간 증가율({max_ratio:.2f}배)이 avg 증가율({avg_ratio:.2f}배)보다 "
            "뚜렷하게 큽니다 — 산발적 정체 구간(GC pause, Redis 순간 지연 등) 가능성. GC 로그/HikariCP/Redis "
            "commandstats를 같이 관찰할 것.\n"
        )
    if cm.get("connect_errors") or cm.get("send_errors"):
        lines.append(f"\n🔴 **자동 플래그**: 에러가 0건이 아닙니다 — 원인 확인 필요.\n")

    lines.append(
        "\n_(자동 생성 — 정성적 해석(동시성 이슈, 병목 가설 등)은 위 수치를 보고 직접 이어서 작성할 것)_\n"
    )
    return "\n".join(lines)


def append_to_analysis(report_md: str):
    ANALYSIS_DIR.mkdir(parents=True, exist_ok=True)
    if not ANALYSIS_FILE.exists():
        ANALYSIS_FILE.write_text(
            "# 채팅 부하테스트 분석 노트\n\n"
            "> `run_and_report.py`가 자동으로 이어서 기록하는 문서. result/analysis 모두 gitignore 대상.\n\n"
            "---\n",
            encoding="utf-8",
        )
    with ANALYSIS_FILE.open("a", encoding="utf-8") as f:
        f.write("\n---\n\n" + report_md)


# ===== k6 실행 =====

def run_k6(script: str, env: dict[str, str]) -> Path:
    """k6를 실행하고, 실행 후 result/ 안에서 새로 생긴 json 파일을 찾아 반환한다."""
    before = {p.resolve() for p in RESULT_DIR.glob("*.json")}

    cmd = ["k6", "run"]
    for k, v in env.items():
        if v is not None:
            cmd += ["-e", f"{k}={v}"]
    cmd.append(script)

    print(f"$ {' '.join(cmd)}", file=sys.stderr)
    subprocess.run(cmd, cwd=REPO_ROOT, check=True)

    after = {p.resolve() for p in RESULT_DIR.glob("*.json")}
    new_files = after - before
    if not new_files:
        raise RuntimeError("k6 실행 후 result/에 새 json 파일을 못 찾음 — handleSummary 경로/라벨을 확인할 것")
    if len(new_files) > 1:
        # 같은 분(minute)에 라벨이 겹치면 발생 가능 — 가장 최근 mtime으로 결정
        return max(new_files, key=lambda p: p.stat().st_mtime)
    return next(iter(new_files))


def cmd_run(args):
    env = {
        "JWT_SECRET": args.jwt_secret,
        "VU_COUNT": str(args.vu) if args.vu else None,
        "DURATION_SEC": str(args.duration) if args.duration else None,
        "RAMP_SEC": str(args.ramp) if args.ramp is not None else None,
        "WS_URL": args.ws_url,
        "MAPPING_FILE": args.mapping_file,
        "LABEL": args.label,
    }
    result_path = run_k6(args.script, env)
    _report(result_path, args.compare_to, args.no_append)


def cmd_report(args):
    _report(Path(args.json), args.compare_to, args.no_append)


def _report(result_path: Path, compare_to: str | None, no_append: bool):
    curr = load_result(result_path)
    prev = find_previous_result(result_path, Path(compare_to) if compare_to else None)
    report_md = render_report(curr, prev)

    print("\n" + report_md)

    if not no_append:
        append_to_analysis(report_md)
        print(f"\n-> {ANALYSIS_FILE} 에 append 완료", file=sys.stderr)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)

    p_run = sub.add_parser("run", help="k6 실행 + 결과 비교 리포트까지 한 번에")
    p_run.add_argument("--script", required=True, help="k6 스크립트 경로 (레포 루트 기준)")
    p_run.add_argument("--jwt-secret", required=True)
    p_run.add_argument("--vu", type=int)
    p_run.add_argument("--duration", type=int, help="DURATION_SEC")
    p_run.add_argument("--ramp", type=int, help="RAMP_SEC")
    p_run.add_argument("--ws-url", help="기본값은 스크립트 내부 값(ws://localhost:8080/api/ws). 원격 배포 테스트 시 여기만 바꾸면 됨")
    p_run.add_argument("--mapping-file", help="예: ./result/loadtest-mapping-200.json")
    p_run.add_argument("--label", help="결과 파일명 라벨(기본은 스크립트의 {VU}vu)")
    p_run.add_argument("--compare-to", help="비교할 이전 결과 json 경로(생략 시 직전 회차 자동 선택)")
    p_run.add_argument("--no-append", action="store_true", help="analysis 문서에 append하지 않고 콘솔 출력만")
    p_run.set_defaults(func=cmd_run)

    p_report = sub.add_parser("report", help="이미 있는 결과 json으로 비교 리포트만 생성")
    p_report.add_argument("--json", required=True)
    p_report.add_argument("--compare-to")
    p_report.add_argument("--no-append", action="store_true")
    p_report.set_defaults(func=cmd_report)

    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
