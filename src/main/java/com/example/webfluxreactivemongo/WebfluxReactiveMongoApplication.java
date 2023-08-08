package com.example.webfluxreactivemongo;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.annotation.Collation;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@SpringBootApplication
public class WebfluxReactiveMongoApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebfluxReactiveMongoApplication.class, args);
	}

}

@Collation
record Feed(@Id String id, @DBRef(lazy = true) Message message) {}
@Collation
record Message(@Id String id, String text ) {}
@Collation
record Text(@Id String id, String text){}

interface FeedRepository extends MongoRepository<Feed, String> {}
interface MessageRepository extends MongoRepository<Message, String>{}
interface TextRepository extends MongoRepository<Text, String>{}

@RestController
@RequestMapping("/feed")
@RequiredArgsConstructor
class FeedRouter {
	final FeedRepository feedRepository;
	final MessageRepository messageRepository;

	@GetMapping
	public ResponseEntity<List<Feed>> fetchAll() {
		var allFeed =  feedRepository.findAll();
		return ResponseEntity.ok(allFeed);
	}

	@GetMapping("/{id}")
	public ResponseEntity<Feed> fetchById(@PathVariable("id") String id) {
		return feedRepository
				.findById(id)
				.map(ResponseEntity::ok)
				.orElseThrow(() -> new NotFoundException());
	}

	@PostMapping
	public ResponseEntity<Feed> saveMessage(@RequestBody Feed feed) {
		return ResponseEntity.ok(feedRepository.save(feed));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Feed> updateMessage(@PathVariable("id") String id, @RequestBody Feed feed) {
		return feedRepository
				.findById(id)
				.map(feedFromDb -> new Feed(feedFromDb.id(), feed.message()))
				.map(feedRepository::save)
				.map(ResponseEntity::ok)
				.orElseThrow(() -> new NotFoundException());
	}

}

@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
class MessageRouter {
	final FeedRepository feedRepository;
	final MessageRepository messageRepository;

	@GetMapping
	public ResponseEntity<List<Message>> fetchAll() {
		return ResponseEntity.ok(messageRepository.findAll());
	}

	@GetMapping("/{id}")
	public ResponseEntity<Message> fetchById(@PathVariable("id") String id) {
		return messageRepository
				.findById(id)
				.map(ResponseEntity::ok)
				.orElseThrow(() -> new NotFoundException());
	}

	@PostMapping
	public ResponseEntity<Message> saveFeed(@RequestBody Message message) {
		return ResponseEntity.ok(messageRepository.save(message));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Message> updateFeed(@PathVariable("id") String id, @RequestBody Message message) {
		return messageRepository
				.findById(id)
				.map(message1 -> new Message(message1.id(), message.text()))
				.map(ResponseEntity::ok)
				.orElseThrow(() -> new NotFoundException());
	}

}

@ResponseStatus(HttpStatus.NOT_FOUND)
class NotFoundException extends RuntimeException {
	public NotFoundException() {
		super();
	}
	public NotFoundException(String message) {
		super(message);
	}
}