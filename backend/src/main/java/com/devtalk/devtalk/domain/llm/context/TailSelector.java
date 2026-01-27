package com.devtalk.devtalk.domain.llm.context;

import com.devtalk.devtalk.domain.message.Message;
import java.util.List;

public interface TailSelector {
    List<Message> selectTail(List<Message> historyInOrder, Message latestUser);
}
