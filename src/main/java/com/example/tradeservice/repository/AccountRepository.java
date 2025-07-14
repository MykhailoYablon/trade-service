package com.example.tradeservice.repository;

import com.example.tradeservice.entity.Account;
import org.springframework.data.repository.CrudRepository;

public interface AccountRepository extends CrudRepository<Account, Long> {
}
