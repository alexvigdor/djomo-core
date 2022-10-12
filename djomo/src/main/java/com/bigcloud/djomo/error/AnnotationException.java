package com.bigcloud.djomo.error;

public class AnnotationException extends RuntimeException {
	public AnnotationException(String string, Exception e) {
		super(string, e);
	}

	private static final long serialVersionUID = -8673809847005929255L;

}
