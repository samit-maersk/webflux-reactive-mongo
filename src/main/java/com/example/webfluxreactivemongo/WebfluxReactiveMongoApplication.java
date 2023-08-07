package com.example.webfluxreactivemongo;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.annotation.Collation;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;

@SpringBootApplication
public class WebfluxReactiveMongoApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebfluxReactiveMongoApplication.class, args);
	}

}

@Collation
record Feed(@Id String id, @DBRef Message message) {}
@Collation
record Message(@Id String id, String text /*, @DBRef List<Text> texts*/) {}
@Collation
record Text(@Id String id, String text){}

interface FeedRepository extends ReactiveMongoRepository<Feed, String>{}
interface MessageRepository extends ReactiveMongoRepository<Message, String>{}
interface TextRepository extends ReactiveMongoRepository<Text, String>{}

@Component
@RequiredArgsConstructor
class Routers {

	final FeedRepository feedRepository;
	final MessageRepository messageRepository;
	final TextRepository textRepository;

	@Bean
	RouterFunction routerFunction() {
		return RouterFunctions
				.route()
				.path("/feed", builder -> builder
						.GET("", this::allFeeds)
						.GET("/{id}", this::feedById)
						.POST("", this::persistFeed)
						.PUT("/{id}",request -> ServerResponse.noContent().build()))
				.path("/message", builder -> builder
						.GET("", this::allMessage)
						.GET("/{id}", this::messageById)
						.POST("", this::persistMessage)
						.PUT("/{id}/reply", request -> ServerResponse.noContent().build())
						.PUT("/{id}",request -> ServerResponse.noContent().build()))
				.build();
	}

	private Mono<ServerResponse> messageById(ServerRequest request) {
		return messageRepository
				.findById(request.pathVariable("id"))
				.flatMap(ServerResponse.ok()::bodyValue);
	}

	private Mono<ServerResponse> allMessage(ServerRequest request) {
		return ServerResponse.ok().body(messageRepository.findAll(), Message.class);
	}

	private Mono<ServerResponse> feedById(ServerRequest request) {
		return feedRepository
				.findById(request.pathVariable("id"))
				.flatMap(ServerResponse.ok()::bodyValue);
	}

	private Mono<ServerResponse> allFeeds(ServerRequest request) {
		var feedWithMessage = feedRepository
				.findAll()
				.flatMap(feed -> messageRepository
						.findById(feed.message().id())
						.map(message -> new Feed(feed.id(), message))
				);

		return ServerResponse.ok().body(feedWithMessage, Feed.class);
	}


	private Mono<ServerResponse> persistMessage(ServerRequest request) {
		return request
				.bodyToMono(Message.class)
				.flatMap(messageRepository::save)
				.flatMap(ServerResponse.ok()::bodyValue);
	}

	private Mono<ServerResponse> persistFeed(ServerRequest request) {
		return request
				.bodyToMono(Feed.class)
				//fetch message and persist it first
				.map(feed -> feed.message())
				.flatMap(messageRepository::save)
				.map(message -> new Feed(null,message))
				//persist the feed
				.flatMap(feedRepository::save)
				.flatMap(ServerResponse.ok()::bodyValue);
	}
}