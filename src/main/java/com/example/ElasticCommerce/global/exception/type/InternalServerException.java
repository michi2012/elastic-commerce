package com.example.ElasticCommerce.global.exception.type;


import com.example.ElasticCommerce.global.exception.BaseException;
import com.example.ElasticCommerce.global.exception.ExceptionType;

public class InternalServerException extends BaseException {

    public InternalServerException(ExceptionType exceptionType) {
        super(exceptionType);
    }

}