package guru.springframework.springairagexpert.services;

import guru.springframework.springairagexpert.model.Answer;
import guru.springframework.springairagexpert.model.Question;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Created by jt, Spring Framework Guru.
 */
@Service
public class OpenAIServiceImpl implements OpenAIService {

    final ChatModel chatModel;
    final VectorStore vectorStore;

    @Value("classpath:/templates/rag-prompt-template.st")
    private Resource ragPromptTemplate;

    @Value("classpath:/templates/system-message-template.st")
    private Resource systemMessageTemplate;

    public OpenAIServiceImpl(ChatModel chatModel, VectorStore vectorStore) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
    }

    @Override
    public Answer getAnswer(Question question) {

        PromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemMessageTemplate);
        Message systemMessage = systemPromptTemplate.createMessage();

        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                .query(question.question()).topK(5).build());
        List<String> contentList = documents.stream().map(Document::getText).toList();

        PromptTemplate promptTemplate = new PromptTemplate(ragPromptTemplate);
        Message userMessage = promptTemplate.createMessage(Map.of("input", question.question(), "documents",
                String.join("\n", contentList)));

        contentList.forEach(System.out::println);

        ChatResponse response = chatModel.call(new Prompt(List.of(systemMessage, userMessage)));

        return new Answer(response.getResult().getOutput().getContent());
    }
}
