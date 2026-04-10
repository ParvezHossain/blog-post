package com.parvez.blogs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class EmailEvent implements Serializable {

    public String to;
    public String subject;
    public String body;
}
