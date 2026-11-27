package team.carrypigeon.backend.chat.domain.features.file.domain.model;

/** 账号占用：已完成文件与未完成上传的声明字节数之和；未上传且已到期的授权不再占用。 */
public record FileStorageUsage(long allocatedBytes, long unfinishedUploads) {}
