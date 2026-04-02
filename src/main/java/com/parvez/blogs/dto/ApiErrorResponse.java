package com.parvez.blogs.dto;

import java.util.List;

public record ApiErrorResponse (String message, List<FieldErrorResponse> errors){
}
