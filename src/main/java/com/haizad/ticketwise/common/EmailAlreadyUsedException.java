package com.haizad.ticketwise.common;

public class EmailAlreadyUsedException extends RuntimeException {

	public EmailAlreadyUsedException() {
		super("That email is already registered");
	}

}
