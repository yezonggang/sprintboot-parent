package com.example.springbootmybatisplus.service.impl;

import com.example.springbootmybatisplus.entity.AccountStateEntity;
import com.example.springbootmybatisplus.mapper.AccountStateMapper;
import com.example.springbootmybatisplus.service.IAccountStateService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author yzg
 * @since 2022-07-17
 */
@Service
public class AccountStateServiceImpl extends ServiceImpl<AccountStateMapper, AccountStateEntity> implements IAccountStateService {

}
