-- 创建库
create database if not exists file_center;

-- 切换库
use file_center;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userName     varchar(256)                           null comment '用户昵称',
    userAccount  varchar(256)                           not null comment '账号',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user / admin',
    userPassword varchar(512)                           not null comment '密码',
    status       int          default '0'               not null  comment '用户状态 0-正常,1-被封禁',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    constraint uni_userAccount
        unique (userAccount)
) comment '用户';

-- 文件表
create table if not exists file
(
    id            bigint auto_increment comment 'id' primary key,
    name          varchar(128)                       null comment '文件名称',
    content       MediumBlob                         null comment '文件内容',
    fileType	  varchar(128)                       not null comment '文件分类',
    createTime    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    userId        bigint                             not null comment '创建用户 id',
    updateTime    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint  default 0                 not null comment '是否删除'
) comment '文件';