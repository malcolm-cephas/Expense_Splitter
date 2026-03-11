package com.malcolm.expensesplitter.services;

import com.malcolm.expensesplitter.config.AppConfig;
import com.malcolm.expensesplitter.models.Group;
import com.malcolm.expensesplitter.models.User;
import com.malcolm.expensesplitter.repositories.GroupRepository;
import com.malcolm.expensesplitter.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service responsible for group management and membership operations.
 */
@Service
@Transactional
public class GroupService {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppConfig appConfig;

    public Group createGroup(String name, String description, UUID createdById) {
        User creator = userRepository.findById(createdById).orElseThrow();
        Group group = new Group(name, description, creator);
        group.getMembers().add(creator);
        return groupRepository.save(group);
    }

    public Group addMemberToGroup(UUID groupId, UUID userId) {
        Group group = groupRepository.findById(groupId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        group.getMembers().add(user);
        return groupRepository.save(group);
    }

    // For legacy support when adding members simply by name
    public Group addMemberToGroupByName(UUID groupId, String memberName) {
        Group group = groupRepository.findById(groupId).orElseThrow();
        // Create user if not exists or find existing (simplified for now)
        String generatedEmail = memberName.toLowerCase().replace(" ", "") + "@example.com";
        User user = userRepository.findByEmail(generatedEmail).orElseGet(() -> {
            User newUser = new User(memberName, generatedEmail, appConfig.getCurrencyCode());
            return userRepository.save(newUser);
        });

        group.getMembers().add(user);
        return groupRepository.save(group);
    }

    public Group removeMemberFromGroup(UUID groupId, UUID userId) {
        Group group = groupRepository.findById(groupId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        group.getMembers().remove(user);
        return groupRepository.save(group);
    }

    public Group updateGroup(UUID groupId, String newName) {
        Group group = groupRepository.findById(groupId).orElseThrow();
        group.setName(newName);
        return groupRepository.save(group);
    }

    public void deleteGroup(UUID groupId) {
        groupRepository.deleteById(groupId);
    }

    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    public Group getGroup(UUID groupId) {
        return groupRepository.findById(groupId).orElseThrow();
    }

    public void updateMemberName(UUID userId, String name) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setName(name);
        userRepository.save(user);
    }
}
