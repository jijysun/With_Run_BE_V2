package UMC_8th.With_Run.chat.service.impl.chatting;

import UMC_8th.With_Run.chat.converter.ChatConverter;
import UMC_8th.With_Run.chat.converter.MessageConverter;
import UMC_8th.With_Run.chat.converter.UserChatConverter;
import UMC_8th.With_Run.chat.dto.ChatRequestDTO;
import UMC_8th.With_Run.chat.dto.ChatResponseDTO;
import UMC_8th.With_Run.chat.entity.Chat;
import UMC_8th.With_Run.chat.entity.Message;
import UMC_8th.With_Run.chat.entity.mapping.UserChat;
import UMC_8th.With_Run.chat.repository.ChatRepository;
import UMC_8th.With_Run.chat.repository.MessageRepository;
import UMC_8th.With_Run.chat.repository.UserChatRepository;
import UMC_8th.With_Run.chat.service.ChatService;
import UMC_8th.With_Run.common.apiResponse.status.ErrorCode;
import UMC_8th.With_Run.common.exception.handler.ChatHandler;
import UMC_8th.With_Run.common.exception.handler.UserHandler;
import UMC_8th.With_Run.user.entity.User;
import UMC_8th.With_Run.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final UserChatRepository userChatRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate template;
    private final RedisTemplate<String, Object> redisTemplate;

    /// followee = 내가 팔로우
    /// follower = 나를 팔로우!


    /*
     * COMMON
     * - x
     *
     * CHAT
     * 2. Chatting -> 읽지 않은 메세지 수 최적화
     */

    @Transactional(readOnly = true)
    public List<ChatResponseDTO.GetChatListDTO> getChatList(User user) {
        List<ChatResponseDTO.GetChatListSQLDTO> chatList = userChatRepository.getChatList(user.getId());

        if (chatList.isEmpty()) throw new ChatHandler(ErrorCode.EMPTY_CHAT_LIST); // 성공 코드로 전환?

        return chatList.stream()
                .map(result -> {
                    List<String> usernameList = Arrays.asList(result.getUsernames().split(","));
                    List<String> profileList = Arrays.asList(result.getProfileImages().split(","));

                    return ChatConverter.toGetChatListDTO(result, usernameList, profileList);
                })
                .collect(Collectors.toList());
    }

    // 채팅 첫 생성 메소드
    @Transactional
    public ChatResponseDTO.CreateChatDTO createChat(Long targetId, User user) {
        User targetUser = userRepository.findByIdWithProfile(targetId).orElseThrow(() -> new UserHandler(ErrorCode.WRONG_USER));

        List<UserChat> privateChat = userChatRepository.findByTwoUserId(user.getId(), targetUser.getId());

        if (!privateChat.isEmpty()) { // 이미 갠톡 존재하는 경우 해당 채팅 입장.
            UserChat userChat = privateChat.get(0);
            userChat.setToChatting();
            Long chatId = userChat.getChat().getId();
            List<ChatResponseDTO.BroadcastMsgDTO> chatHistoryDTO = MessageConverter.toChatHistoryDTO(messageRepository.findByChat_Id(chatId), chatId);
            return ChatConverter.toCreateChatDTO(chatId, chatHistoryDTO); // join fetch!
        }


        Chat chat = ChatConverter.toNewChatConverter();

        List<UserChat> userChats = new ArrayList<>();
        UserChat newUserChat = UserChatConverter.toNewUserChat(user, targetUser, null, chat);
        newUserChat.setToChatting();
        userChats.add(newUserChat);
        userChats.add(UserChatConverter.toNewUserChat(targetUser, user, null,  chat));

        chat.addUserChat(userChats.get(0));
        chat.addUserChat(userChats.get(1));

        Chat saveChat = chatRepository.save(chat);
        Long chatId = saveChat.getId();
        userChatRepository.saveAll(userChats);

        ///  메세지 보내기!!
        // redis 처리 전용 dto 변환,
        messageRepository.save(MessageConverter.toFirstChatMessage(user, chat));
        return ChatConverter.toCreateChatDTO(chatId, MessageConverter.toChatHistoryDTO(messageRepository.findByChat_Id(chatId), chatId));
    }

    // 채팅방 이름 변경 메소드, 전체 공통 변경
    @Transactional
    public ChatResponseDTO.RenameChatDTO renameChat(Long chatId, String newName, User user) {
        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new ChatHandler(ErrorCode.WRONG_CHAT));

        UserChat userchat = userChatRepository.findByUser_IdAndChat_Id(user.getId(), chat.getId()).orElseThrow(() -> new UserHandler(ErrorCode.WRONG_USER));

        log.info("newName : " + newName);

        userchat.renameChat(newName);

        return ChatResponseDTO.RenameChatDTO.builder()
                .chatId(chatId)
                .chatName(newName)
                .build();
    }

    // 채팅 초대 목록 조회 리스트
    public List<ChatResponseDTO.GetInviteUserDTO> getInviteUser(Long chatId, User user) {

        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new ChatHandler(ErrorCode.WRONG_CHAT));

        // 이미 채팅방이 자신 포함 4명이라면 초대 목록 조회도 거절
        if (chat.getParticipants() >= 4) {
            throw new ChatHandler(ErrorCode.CHAT_IS_FULL);
        }

        log.info("canInviteUser!");
        List<ChatResponseDTO.GetInviteUserDTO> canInviteUser = userChatRepository.getCanInviteUser(user.getId(), chatId);
        for (ChatResponseDTO.GetInviteUserDTO getInviteUserDTO : canInviteUser) {
            log.info("{}, {}", getInviteUserDTO.getUserId(), getInviteUserDTO.getName());
        }
        return canInviteUser;
    }

    // 위 메소드에서 조회한 사용자 초대 메소드
    @Transactional
    public void inviteUser(Long chatId, ChatRequestDTO.InviteUserReqDTO reqDTO, User user) {

        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new ChatHandler(ErrorCode.WRONG_CHAT));

        List<ChatRequestDTO.InviteDTO> inviteUserList = reqDTO.getInviteUserList();

        for (ChatRequestDTO.InviteDTO inviteDTO : inviteUserList) {
            log.info("invited user : {}, {}", inviteDTO.getUserId(), inviteDTO.getName());
        }

        if (chat.getParticipants() + inviteUserList.size() > 4)
            throw new ChatHandler(ErrorCode.CANT_INVITE_MORE_FOUR);

        // 초대된 사용자 조회 - start
        List<Long> invitedUserIdList = inviteUserList.stream()
                .map(ChatRequestDTO.InviteDTO::getUserId)
                .toList();
        List<User> allInvitedUserList = userRepository.findAllById(invitedUserIdList);
        Map<Long, User> invitedUserMap = allInvitedUserList.stream()
                .collect(Collectors.toMap(User::getId, invitedUser -> invitedUser));

        if (userChatRepository.findAlreadyInvited(chatId, invitedUserIdList)) // 이미 초대된 사용자 초대 시도 시
            throw new UserHandler(ErrorCode.ALREADY_PARTICIPATING);

        chat.updateParticipants(chat.getParticipants() + inviteUserList.size());

        // 기존 사용자
        List<UserChat> userChatList = userChatRepository.findAllByChat_IdJoinFetchUserAndProfile(chatId);
        List<String> existingUserNames = userChatList.stream()
                .map(userChat -> userChat.getUser().getProfile().getName())
                .collect(Collectors.toList());

        List<String> newUserNames = inviteUserList.stream()
                .map(ChatRequestDTO.InviteDTO::getName)
                .collect(Collectors.toList());

        // 전체 참여자 이름 목록
        List<String> allParticipantNames = new ArrayList<>();
        allParticipantNames.addAll(existingUserNames);
        allParticipantNames.addAll(newUserNames);

        List<UserChat> newUserChat = new ArrayList<>();
        for (ChatRequestDTO.InviteDTO dto : inviteUserList) { // 초대된 사용자 정보 만큼

            User thisUser = invitedUserMap.get(dto.getUserId());

            String chatName = allParticipantNames.stream() // 자신을 제외한 다른 사용자 이름들로 채팅방 이름 생성
                    .filter(name -> !name.equals(thisUser.getProfile().getName()))
                    .collect(Collectors.joining(", "));

            newUserChat.add(UserChatConverter.toNewUserChat(thisUser, null, chatName, chat));
        }
        userChatRepository.saveAll(newUserChat);

        log.info("all user name in chat: {}", allInvitedUserList);

        // 기존 사용자 update
        for (UserChat userChat : userChatList) { // 기존 사용자의 user_chat 에 대해
            if (!userChat.getIsDefaultChatName()) continue; // 각 유저에 대해 커스텀 ChatName 이 아닌 경우 Updat

            String currentUserName = userChat.getUser().getProfile().getName();


            Long currentUserId = userChat.getUser().getId();

            String otherName = allParticipantNames.stream()
                    .filter(name -> !name.equals(currentUserName))
                    .collect(Collectors.joining(", "));

            log.info("{}'s name = {}", currentUserId, otherName);

            userChat.renameDefaultChatName(otherName);
        }

        List<String> nameList = inviteUserList.stream()
                .map(ChatRequestDTO.InviteDTO::getName).toList();

        String name = "";
        for (String s : nameList) {
            name += s + "님 ";
        }
        name = name.substring(0, name.length() - 1);

        // 채팅방에 초대 메세지 뿌리기 + save
        String inviteMsg = reqDTO.getUsername() + "님이 " + name + "을 초대하였습니다.";
        messageRepository.save(MessageConverter.toInviteMessage(user, chat, inviteMsg));
        template.convertAndSend("/sub/" + chatId + "/msg", inviteMsg);
    }

    @Transactional
    public List<ChatResponseDTO.BroadcastMsgDTO> enterChat(Long chatId, User user) { // 메세지에 대한 대량의 입출력, MySQL 로는 무겁지 않을까요...
        UserChat userChat = userChatRepository.findByUser_IdAndChat_Id(user.getId(), chatId).orElseThrow(() -> new ChatHandler(ErrorCode.WRONG_CHAT));

        // 사용자의 읽지 않은 메세지 수 0 + isChatting = true -> redis
        userChat.setToChatting();

        return MessageConverter.toChatHistoryDTO(messageRepository.findByChat_Id(chatId), chatId); // join fetch!
    }

    @Override
    @Transactional
    public List<ChatResponseDTO.BroadcastMsgDTO> getChatHistory(Long chatId, Long cursor, User user) {

        UserChat uc = userChatRepository.findByUser_IdAndChat_Id(user.getId(), chatId).orElseThrow(() -> new ChatHandler(ErrorCode.WRONG_CHAT));
        uc.setToChatting();

        PageRequest page = PageRequest.of(0, 30);

        if (cursor == null) {
            List<Message> lastestMessageList = messageRepository.getLastestMessagesByChatId(chatId, uc.getCreatedAt(), page);
            return MessageConverter.toChatHistoryDTO(lastestMessageList, chatId);
        }
        else {
            List<Message> previousMessageList = messageRepository.getPreviousMessagesByChatId(chatId, uc.getCreatedAt(), cursor, page);

            if (previousMessageList.isEmpty())
                throw new ChatHandler(ErrorCode.NO_MORE_MESSAGE);
            return MessageConverter.toChatHistoryDTO(previousMessageList, chatId);
        }
    }


    @Transactional
    public void leaveChat(Long chatId, User user) {
        UserChat userChat = userChatRepository.findByUser_IdAndChat_Id(user.getId(), chatId).orElseThrow(() -> new ChatHandler(ErrorCode.WRONG_CHAT));
        userChat.setToNotChatting(); // -> redis
    }

    @Transactional
    public void deleteChat(Long chatId, User user) {
        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new ChatHandler(ErrorCode.WRONG_CHAT));
        UserChat userChat = userChatRepository.findByUser_IdAndChat_Id(user.getId(), chatId).orElseThrow(() -> new ChatHandler(ErrorCode.WRONG_CHAT));

        
        // redis 삭제 로직 추가
        userChatRepository.delete(userChat);

        Integer participants = chat.getParticipants();

        if (participants - 1 == 0) {
            chatRepository.deleteById(chatId);
        } else {
            chat.updateParticipants(participants - 1);
        }
    }
}