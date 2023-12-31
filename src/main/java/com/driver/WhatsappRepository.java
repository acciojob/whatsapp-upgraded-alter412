package com.driver;

import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class WhatsappRepository {

    int ithM;

    int gCount;
    private Map<String,User> usersDB;
    private Map<Integer,Message> messagesDB;
    private Map<String,Group> groupsDB;

    private Map<String,String> groupAdminMap;
    private Map<String,List<String>> groupUsersMap;

    private Map<String, List<Integer>> groupMessagesMap;

    private Map<String,List<Integer>> userMessageMap;

    public WhatsappRepository() {
        this.ithM = 0;
        this.gCount = 0;
        this.usersDB = new HashMap<>();
        this.messagesDB = new HashMap<>();
        this.groupsDB= new HashMap<>();
        this.groupAdminMap = new HashMap<>();
        this.groupUsersMap = new HashMap<>();
        this.groupMessagesMap = new HashMap<>();
        this.userMessageMap = new HashMap<>();
    }

    public void createUser(String name, String mobile) throws Exception {
        if(usersDB.containsKey(name)){
            throw new Exception("User already exists");
        }
        User user = new User();
        user.setName(name);
        user.setMobile(mobile);
        usersDB.put(name,user);
    }

    public Group createGroup(List<User> users) {
        // The list contains at least 2 users where the first user is the admin. A group has exactly one admin.
        // If there are only 2 users, the group is a personal chat and the group name should be kept as the name of the second user(other than admin)
        // If there are 2+ users, the name of group should be "Group count". For example, the name of first group would be "Group 1", second would be "Group 2" and so on.
        // Note that a personal chat is not considered a group and the count is not updated for personal chats.
        // If group is successfully created, return group.

        if(users.size()==2){
            Group group = new Group();
            group.setName(users.get(1).getName());
            group.setNumberOfParticipants(2);
            this.groupsDB.put(users.get(1).getName(),group);
            this.groupUsersMap.put(group.getName(),users.stream().map(user -> user.getName()).collect(Collectors.toList()));
            this.groupAdminMap.put(group.getName(),users.get(0).getName());
            return group;
        }else if(users.size()>2){
            Group group = new Group();
            this.gCount++;
            group.setName("Group "+this.gCount);
            group.setNumberOfParticipants(users.size());
            this.groupsDB.put(group.getName(),group);
            this.groupAdminMap.put(group.getName(),users.get(0).getName());
            this.groupUsersMap.put(group.getName(),users.stream().map(user -> user.getName()).collect(Collectors.toList()));
            return group;
        }
        return null;
    }

    public int createMessage(String content) {
        this.ithM++;
        Message message = new Message();
        message.setId(ithM);
        message.setContent(content);
        message.setTimestamp(new Date());

        this.messagesDB.put(message.getId(),message);

        return message.getId();
    }

    public int sendMessage(Message message, User sender, Group group) throws Exception {
        //Throw "Group does not exist" if the mentioned group does not exist
        //Throw "You are not allowed to send message" if the sender is not a member of the group
        //If the message is sent successfully, return the final number of messages in that group.
        if(!groupsDB.containsKey(group.getName())){
            throw new Exception("Group does not exist");
        }
        if(!groupUsersMap.get(group.getName()).contains(sender.getName())){
            throw new Exception("You are not allowed to send message");
        }
        if(!this.groupMessagesMap.containsKey(group.getName())){
          this.groupMessagesMap.put(group.getName(), new ArrayList<>());
        }
        this.groupMessagesMap.get(group.getName()).add(message.getId());
        if(!this.userMessageMap.containsKey(sender.getName())){
            this.userMessageMap.put(sender.getName(), new ArrayList<>());
        }
        this.userMessageMap.get(sender.getName()).add(message.getId());
        return this.groupMessagesMap.get(group.getName()).size();
    }

    public String changeAdmin(User approver, User user, Group group)  throws Exception{
        //Throw "Group does not exist" if the mentioned group does not exist
        //Throw "Approver does not have rights" if the approver is not the current admin of the group
        //Throw "User is not a participant" if the user is not a part of the group
        //Change the admin of the group to "user" and return "SUCCESS". Note that at one time there is only one admin and the admin rights are transferred from approver to user.

        if(!groupsDB.containsKey(group.getName())){
            throw new Exception("Group does not exist");
        }

        if(!groupAdminMap.get(group.getName()).equals(approver.getName())){
            throw new Exception("Approver does not have rights");
        }

        if(!groupUsersMap.get(group.getName()).contains(user.getName())){
            throw new Exception("User is not a participant");
        }

        groupAdminMap.put(group.getName(), user.getName());
        return "SUCCESS";
    }

    public int removeUser(User user) throws Exception {
        //A user belongs to exactly one group
        //If user is not found in any group, throw "User not found" exception
        //If user is found in a group and it is the admin, throw "Cannot remove admin" exception
        //If user is not the admin, remove the user from the group, remove all its messages from all the databases, and update relevant attributes accordingly.
        //If user is removed successfully, return (the updated number of users in the group + the updated number of messages in group + the updated number of overall messages)
        boolean found = false;
        String fgroup = null;
        for(String group : groupUsersMap.keySet()){
            for(String temp : groupUsersMap.get(group)){
                if(temp.equals(user.getName())){
                    found=true;
                    fgroup=group;
                    break;
                }
            }
            if(found){
                break;
            }
        }

        if(!found){
            throw new Exception("User not found");
        }
        if(groupAdminMap.get(fgroup).equals(user.getName())){
            throw new Exception("Cannot remove admin");
        }
        List<Integer> messages = userMessageMap.get(user.getName());
        userMessageMap.remove(user.getName());
        usersDB.remove(user.getName());
        groupUsersMap.get(fgroup).remove(user.getName());
        for(int id : messages){
            messagesDB.remove(id);
            groupMessagesMap.get(fgroup).remove(Integer.valueOf(id));
        }

        groupsDB.get(fgroup).setNumberOfParticipants(groupsDB.get(fgroup).getNumberOfParticipants()-1);

        return groupUsersMap.get(fgroup).size() + groupMessagesMap.get(fgroup).size() + messagesDB.size() +1 ;
    }
}
