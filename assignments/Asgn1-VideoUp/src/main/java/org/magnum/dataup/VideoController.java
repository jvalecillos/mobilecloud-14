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
package org.magnum.dataup;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hamcrest.core.IsNull;
import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class VideoController {
	// An in-memory list that the controller uses to store the
	// videos that are sent to it by clients
	//private Collection<Video> videos = new ArrayList<Video>();
	private static final AtomicLong currentId = new AtomicLong(0L);
    private Map<Long,Video> videos = new HashMap<Long, Video>();
	
	/**
	 * Returns the list of videos that have been added to the 
	 * server as JSON. The list of videos does not have to be
	 * persisted across restarts of the server
	 * 
	 * @return Returns the JSON representation of the video's list
	 */
	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH, method = RequestMethod.GET)
    public @ResponseBody Collection<Video> getVideos() {
        return this.videos.values();
    }
	
	
	/**
	 * Creates a video entry in the video's list and returns the video object
	 * 
	 * @param v Video details
	 * @return Returns the JSON representation of the Video
	 */
	@RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH, method = RequestMethod.POST)
	public @ResponseBody Video createVideo(@RequestBody Video v) {
		// Set an unique id for the video
		checkAndSetId(v);
		// Set data url from its id
		v.setDataUrl(this.getDataUrl(v.getId()));
		// Add video to the list
        videos.put(v.getId(), v);
		return v;
	}
	
	@RequestMapping(value = VideoSvcApi.VIDEO_DATA_PATH, method = RequestMethod.POST)
    public @ResponseBody VideoStatus uploadVideo (
               @PathVariable("id") long id, 
               @RequestParam("data") MultipartFile data, 
               HttpServletResponse response) {
		Video v = videos.get(id);
		// if no video was found
		if ( v == null ) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		try {
			VideoFileManager.get().saveVideoData(v, data.getInputStream());
			
			if (VideoFileManager.get().hasVideoData(v))
				return new VideoStatus(VideoState.READY);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
    }
	
	@RequestMapping(value = VideoSvcApi.VIDEO_DATA_PATH, method = RequestMethod.GET)
    public void getVideo(
               @PathVariable("id") long id, 
               HttpServletResponse response) {
		Video v = videos.get(id);
		
		try {
			
			if (v != null && VideoFileManager.get().hasVideoData(v)) {
				VideoFileManager.get().copyVideoData(v, response.getOutputStream());
			} else {
				// if no video or video file was found
				response.setStatus(HttpStatus.NOT_FOUND.value());
			}
			
				
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	
	private void checkAndSetId(Video entity) {
        if(entity.getId() == 0){
            entity.setId(currentId.incrementAndGet());
        }
    }
	
	private String getDataUrl(long videoId){
		String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
		return url;
	}
	
	private String getUrlBaseForLocalServer() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
				.getRequestAttributes()).getRequest();
		String base = "http://"
				+ request.getServerName()
				+ ((request.getServerPort() != 80) ? ":"
						+ request.getServerPort() : "");
		return base;
	}
}
