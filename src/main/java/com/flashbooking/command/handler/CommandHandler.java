package com.flashbooking.command.handler;

import com.flashbooking.command.model.Command;

public interface CommandHandler<C extends Command, R> {
  R handle(C command);
}
