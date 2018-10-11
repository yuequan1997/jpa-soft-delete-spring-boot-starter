package org.yuequan.jpa.soft.delete.mock.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.yuequan.jpa.soft.delete.mock.entity.User;
import org.yuequan.jpa.soft.delete.repository.SoftDelete;

@SoftDelete
public interface UserRepository extends JpaRepository<User, Integer> {

}
