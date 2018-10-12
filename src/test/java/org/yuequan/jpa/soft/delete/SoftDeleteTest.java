package org.yuequan.jpa.soft.delete;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.yuequan.jpa.soft.delete.mock.entity.User;
import org.yuequan.jpa.soft.delete.mock.repository.UserRepository;
import org.yuequan.jpa.soft.delete.repository.SoftDelete;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@RunWith(SpringRunner.class)
@Profile("test")
@SpringBootTest
public class SoftDeleteTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @Transactional
    public void testSoftDelete(){
        User user = getUser();
        userRepository.save(user);
        userRepository.delete(user);
        Assert.assertEquals(userRepository.findById(user.getId()), Optional.empty());
        int createUserCount = 100;

        List<User> users = getUsers(createUserCount);
        userRepository.saveAll(users);
        Assert.assertEquals(createUserCount, userRepository.findAll().size());
        userRepository.deleteAllInBatch();
        Assert.assertEquals(0, userRepository.findAll().size());

        users = getUsers(createUserCount);
        userRepository.saveAll(users);
        users = userRepository.findAll();
        Assert.assertEquals(createUserCount, userRepository.findAll(PageRequest.of(0, createUserCount)).getTotalElements());
        userRepository.deleteInBatch(users);
        Assert.assertEquals(0, userRepository.findAll().size());
    }



    private User getUser(){
        User user = new User();
        user.setUsername("Tester" + new Random().nextInt(1000));
        user.setName("Tester");
        user.setPassword("test");
        return user;
    }

    private List<User> getUsers(int createUserCount){
        List<User> users = new ArrayList<>();
        for (int i = 0; i < createUserCount; i++) {
            users.add(getUser());
        }
        return users;
    }
}
