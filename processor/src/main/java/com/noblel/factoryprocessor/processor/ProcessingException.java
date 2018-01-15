package com.noblel.factoryprocessor.processor;

import javax.lang.model.element.Element;

/**
 * 注解处理异常
 *
 * @author Noblel
 */
public class ProcessingException extends Exception{
    Element element;

    public ProcessingException(Element element, String msg, Object... args) {
        super(String.format(msg, args));
        this.element = element;
    }

    public Element getElement() {
        return element;
    }
}
