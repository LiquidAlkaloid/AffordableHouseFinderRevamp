package com.example.affordablehousefinderrevamp;

public class User {
    private String id;
    private String name;
    private String email;
    private String password;
    private String phone;
    private String address;
    private String userType; // "buyer" or "seller"

    public User() {
    }

    public User(String id, String name, String email, String phone, String address, String userType) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.userType = userType;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getUserType() {
        return userType;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }
}