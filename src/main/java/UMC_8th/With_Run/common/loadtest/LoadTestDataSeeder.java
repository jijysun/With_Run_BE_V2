package UMC_8th.With_Run.common.loadtest;

import UMC_8th.With_Run.chat.converter.UserChatConverter;
import UMC_8th.With_Run.chat.entity.Chat;
import UMC_8th.With_Run.chat.entity.mapping.UserChat;
import UMC_8th.With_Run.chat.repository.ChatRepository;
import UMC_8th.With_Run.user.entity.Profile;
import UMC_8th.With_Run.user.entity.User;
import UMC_8th.With_Run.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "loadtest.seed", name = "enabled", havingValue = "true")
public class LoadTestDataSeeder implements ApplicationRunner {

    private static final String LOGIN_ID_PREFIX = "loadtest_";
    private static final int TARGET_USER_COUNT = 500;
    private static final int ROOM_CAPACITY = 4;

    private final UserRepository userRepository;
    private final ChatRepository chatRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long existingUserCount = userRepository.countByLoginIdStartingWith(LOGIN_ID_PREFIX);
        int createdUserCount = 0;

        if (existingUserCount < TARGET_USER_COUNT) {
            createdUserCount = createUsers((int) existingUserCount, TARGET_USER_COUNT);
        }

        int createdRoomCount = assignRooms();

        log.info("[LoadTestDataSeeder] 유저 {}명 신규 생성(기존 {}명), 채팅방 {}개 신규 생성",
                createdUserCount, existingUserCount, createdRoomCount);
    }

    private int createUsers(int fromExclusive, int toInclusive) {
        List<User> users = new ArrayList<>();

        for (int i = fromExclusive + 1; i <= toInclusive; i++) {
            String suffix = String.format("%03d", i);
            String loginId = LOGIN_ID_PREFIX + suffix;

            Profile profile = Profile.builder()
                    .name("부하 테스트 유저" + suffix)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            User user = User.builder()
                    .loginId(loginId)
                    .email(loginId + "@loadtest.local")
                    .createdAt(LocalDate.now())
                    .updatedAt(LocalDate.now())
                    .noticeEnabled(true)
                    .profile(profile)
                    .build();
            profile.setUser(user);

            users.add(user);
        }

        userRepository.saveAll(users);
        return users.size();
    }

    private int assignRooms() {
        List<User> unassigned = userRepository.findUnassignedByLoginIdPrefix(LOGIN_ID_PREFIX);
        int roomsCreated = 0;

        for (int i = 0; i + ROOM_CAPACITY <= unassigned.size(); i += ROOM_CAPACITY) {
            List<User> members = unassigned.subList(i, i + ROOM_CAPACITY);

            Chat chat = Chat.builder()
                    .participants(ROOM_CAPACITY)
                    .userChatList(new ArrayList<>())
                    .messageList(new ArrayList<>())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            for (User member : members) {
                UserChat userChat = UserChatConverter.toNewUserChat(member, null, "로드테스트방", chat);
                chat.addUserChat(userChat);
            }

            chatRepository.save(chat);
            roomsCreated++;
        }

        return roomsCreated;
    }
}
