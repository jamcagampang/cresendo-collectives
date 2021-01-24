package com.jcagampang.cresendo.yelpreivew.controller;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.jcagampang.cresendo.yelpreivew.service.YelpReviewService;
import com.jcagampang.cresendo.yelpreivew.util.Constants;

@RestController
public class YelpReviewController {

	@Autowired
	YelpReviewService yelpReviewService;

	@GetMapping("/get-review/{id}")
	public ResponseEntity<String> getYelpReview(@PathVariable(value = "id") String id) {

		JSONObject response = new JSONObject();

		try {
			response.put(Constants.REVIEWS, yelpReviewService.getAllReviewsWithEmotion(id));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.ok(response.toString());
		}

		return ResponseEntity.ok(response.toString());
	}
}
