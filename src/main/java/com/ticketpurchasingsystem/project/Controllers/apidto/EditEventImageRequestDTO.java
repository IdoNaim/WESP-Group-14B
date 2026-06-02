package com.ticketpurchasingsystem.project.Controllers.apidto;

public class EditEventImageRequestDTO {
    private String newImageUrl;

    public EditEventImageRequestDTO() {}

    public String getNewImageUrl() { return newImageUrl; }
    public void setNewImageUrl(String newImageUrl) { this.newImageUrl = newImageUrl; }
}