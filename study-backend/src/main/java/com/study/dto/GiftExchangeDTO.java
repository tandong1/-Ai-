package com.study.dto;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.*;
@Getter @Setter
public class GiftExchangeDTO {
 @NotNull public Long giftId;
 @NotBlank @Size(max=64) @Pattern(regexp="[A-Za-z0-9_-]+") public String requestId;
 @Size(max=80) public String recipient;
 @Size(max=40) public String phone;
 @Size(max=500) public String address;
 @Size(max=120) public String account;
 @Size(max=200) public String password;
 @Size(max=100) public String region;
 @Size(max=100) public String gameName;
 @Size(max=500) public String remark;
}
