package com.devtalk.devtalk.service.devtalk.llm.context;

import com.devtalk.devtalk.domain.message.Message;
import java.util.List;

public interface TailSelector {
    List<Message> selectTail(List<Message> historyInOrder, Message latestUser);
}
