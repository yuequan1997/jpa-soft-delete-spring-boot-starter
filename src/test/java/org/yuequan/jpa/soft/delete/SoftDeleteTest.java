package org.yuequan.jpa.soft.delete;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.yuequan.jpa.soft.delete.mock.entity.User;
import org.yuequan.jpa.soft.delete.mock.repository.UserRepository;
import org.yuequan.jpa.soft.delete.repository.SoftDelete;

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
    }

    private User getUser(){
        User user = new User();
        user.setUsername("Tester" + new Random().nextInt(1000));
        user.setName("Tester");
        user.setPassword("test");
        return user;
    }
}
