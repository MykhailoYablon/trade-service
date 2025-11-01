package com.strategy.trade.repository;

import com.strategy.trade.entity.Account;
import org.springframework.data.repository.CrudRepository;

public interface AccountRepository extends CrudRepository<Account, Long> {
}
