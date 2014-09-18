/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package org.magnum.mobilecloud.video;

import java.security.Principal;
import java.util.Collection;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.magnum.mobilecloud.video.client.VideoSvcApi;
import org.magnum.mobilecloud.video.repository.Video;
import org.magnum.mobilecloud.video.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import retrofit.http.Path;

import com.google.common.collect.Lists;

@Controller
public class VideoController {
	
	// The VideoRepository that we are going to store our videos
	// in. We don't explicitly construct a VideoRepository, but
	// instead mark this object as a dependency that needs to be
	// injected by Spring. Our Application class has a method
	// annotated with @Bean that determines what object will end
	// up being injected into this member variable.
	//
	// Also notice that we don't even need a setter for Spring to
	// do the injection.
	//
	@Autowired
	private VideoRepository videos;
	
	// Receives POST requests to /video and converts the HTTP
	// request body, which should contain json, into a Video
	// object before adding it to the list.
	//
	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH, method = RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v) {
		v.setLikes(0);
		return videos.save(v);
	}

	// Receives GET requests to /video and returns the current
	// list of videos.
	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH, method = RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList() {
		return Lists.newArrayList(videos.findAll());
	}
	
	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH + "/{id}", method = RequestMethod.GET)
	public @ResponseBody Video getVideoById(
			@PathVariable("id") long id, 
			HttpServletResponse response) {
		Video v = videos.findOne(id);
		if (v == null)
			response.setStatus(HttpStatus.NOT_FOUND.value());

		return v;
				
	}
	
	// Receives GET requests to /video/find and returns all Videos
	// that have a title (e.g., Video.name) matching the "title" request
	// parameter value that is passed by the client
	@RequestMapping(value = VideoSvcApi.VIDEO_TITLE_SEARCH_PATH, method = RequestMethod.GET)
	public @ResponseBody Collection<Video> findByTitle(
	// Tell Spring to use the "title" parameter in the HTTP request's query
	// string as the value for the title method parameter
			@RequestParam(VideoSvcApi.TITLE_PARAMETER) String title) {
		return videos.findByName(title);
	}
	
	@RequestMapping(value = VideoSvcApi.VIDEO_DURATION_SEARCH_PATH, method = RequestMethod.GET)
	public @ResponseBody Collection<Video> findByDurationLessThan(
			@RequestParam(VideoSvcApi.DURATION_PARAMETER) long duration) {
		return videos.findByDurationLessThan(duration);
	}
	
	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH + "/{id}/like", method = RequestMethod.POST)
	public @ResponseBody void likeVideo(
			@PathVariable("id") long id, 
			HttpServletResponse response, Principal p) {

		Video v = videos.findOne(id);
		// checks if video exists
		if (v == null) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
			return;
		}
		
		Set<String> usersLikes = v.getUsersLikes();
		// Get logged username
		String username = p.getName();
		// Checks if the user has already liked the video.
		if (usersLikes.contains(username)) {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			return;
		}
		
		// add user to liked video list
		usersLikes.add(username);
		v.setUsersLikes(usersLikes);
		// update likes count
		v.setLikes(usersLikes.size());
		// saving video
		videos.save(v);
	}
	
	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH + "/{id}/unlike", method = RequestMethod.POST)
	public @ResponseBody void unlikeVideo(@PathVariable("id") long id, 
			HttpServletResponse response, Principal p) {

		Video v = videos.findOne(id);
		// checks if video exists
		if (v == null) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
			return;
		}
		
		Set<String> usersLikes = v.getUsersLikes();
		// Get logged username
		String username = p.getName();
		// Checks if the user has already liked the video.
		if (!usersLikes.contains(username)) {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			return;
		}
		
		// add user to liked video list
		usersLikes.remove(username);
		v.setUsersLikes(usersLikes);
		// update likes count
		v.setLikes(usersLikes.size());
		// saving video
		videos.save(v);
	}
	
	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH + "/{id}/likedby", method = RequestMethod.GET)
	public @ResponseBody Collection<String> getUsersWhoLikedVideo(@PathVariable("id") long id, 
			HttpServletResponse response, Principal p) {

		Video v = videos.findOne(id);
		// checks if video exists
		if (v == null) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		
		Set<String> usersLikes = v.getUsersLikes();
		
		return usersLikes;
		
	}
	
	
}