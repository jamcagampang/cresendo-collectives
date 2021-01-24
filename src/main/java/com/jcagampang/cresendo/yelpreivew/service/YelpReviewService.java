package com.jcagampang.cresendo.yelpreivew.service;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.gcp.vision.CloudVisionTemplate;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.FaceAnnotation;
import com.google.cloud.vision.v1.Feature.Type;
import com.jcagampang.cresendo.yelpreivew.model.Review;
import com.jcagampang.cresendo.yelpreivew.util.Constants;

@Service
public class YelpReviewService {

	public static final String REVIEWS_URL = "https://api.yelp.com/v3/businesses/{id}/reviews";

	@Value("${yelp-api-key}")
	private String yelpApiKey;

	@Autowired
	private ResourceLoader resourceLoader;

	@Autowired
	private CloudVisionTemplate cloudVisionTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	private RestTemplate restTemplate;

	public YelpReviewService(RestTemplateBuilder restTemplateBuilder) {
		this.restTemplate = restTemplateBuilder.build();
	}

	/**
	 * Retrieves all the reviews along with user's emotion.
	 * 
	 * @param id - Unique ID of the Business
	 * @return <code>List</code> of <code>Review</code> <code>JSONObjects</code>
	 * @throws JsonMappingException
	 * @throws JsonProcessingException
	 * @throws JSONException
	 */
	public List<JSONObject> getAllReviewsWithEmotion(String id)
			throws JsonMappingException, JsonProcessingException, JSONException {

		List<Review> reviews = this.getAllReviews(id);
		List<JSONObject> reviewsWithEmotions = new ArrayList<JSONObject>();

		// Convert Review POJOs to JSONObject and include emotion data.
		for (Review review : reviews) {

			String reviewString = objectMapper.writeValueAsString(review);

			JSONObject reviewJSON = new JSONObject(reviewString);

			/*
			 * Use Google Vision API to retrieve emotion from user's avatar.
			 */
			AnnotateImageResponse response = this.cloudVisionTemplate.analyzeImage(
					this.resourceLoader.getResource(review.getUser().getImage_url()), Type.FACE_DETECTION);

			JSONArray emotions = new JSONArray();

			/*
			 * An image can contain multiple faces, so add em' all.
			 */
			List<FaceAnnotation> faceAnnotationsList = response.getFaceAnnotationsList();

			for (FaceAnnotation faceAnnotation : faceAnnotationsList) {

				JSONObject emotion = new JSONObject();

				emotion.put(Constants.JOY_LIKELIHOOD, faceAnnotation.getJoyLikelihoodValue());
				emotion.put(Constants.SORROW_LIKELIHOOD, faceAnnotation.getSorrowLikelihoodValue());
				emotion.put(Constants.ANGER_LIKELIHOOD, faceAnnotation.getAngerLikelihoodValue());
				emotion.put(Constants.SURPRISE_LIKELIHOOD, faceAnnotation.getSurpriseLikelihoodValue());

				emotions.put(emotion);
			}

			reviewJSON.put(Constants.EMOTIONS, emotions);

			reviewsWithEmotions.add(reviewJSON);
		}

		return reviewsWithEmotions;
	}

	/**
	 * Retrieves all the reviews of the given business.
	 * 
	 * @param id - Unique ID of the Business
	 * @return <code>List</code> of <code>Review</code>s
	 * @throws JSONException
	 * @throws JsonMappingException
	 * @throws JsonProcessingException
	 */
	public List<Review> getAllReviews(String id) throws JSONException, JsonMappingException, JsonProcessingException {

		HttpHeaders httpHeaders = new HttpHeaders();

		// Attaching the Yelp's App API Key for authentication.
		httpHeaders.setBearerAuth(yelpApiKey);

		HttpEntity<String> request = new HttpEntity<String>(httpHeaders);

		ResponseEntity<String> response = this.restTemplate.exchange(REVIEWS_URL, HttpMethod.GET, request, String.class,
				id);

		JSONObject responseJSON = new JSONObject(response.getBody());

		JSONArray reviewArray = responseJSON.getJSONArray(Constants.REVIEWS);

		// Maps Review JSONs to Review POJOs
		List<Review> reviews = objectMapper.readValue(reviewArray.toString(), new TypeReference<List<Review>>() {
		});

		return reviews;
	}
}
