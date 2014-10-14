
package org.magnum.mobilecloud.video;

import java.util.Collection;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.magnum.mobilecloud.video.client.VideoSvcApi;
import org.magnum.mobilecloud.video.repository.Video;
import org.magnum.mobilecloud.video.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Lists;

/**
 * This simple VideoController allows clients to send HTTP POST requests with
 * videos that are stored in memory using a list. Clients can send HTTP GET
 * requests to receive a JSON listing of the videos that have been sent to
 * the controller so far. Stopping the controller will cause it to lose the history of
 * videos that have been sent to it because they are stored in memory.
 * 
 * @author javiervazquez
 *
 */

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
		
	// Receives GET requests to /video and returns the current
	// list of videos in memory. Spring automatically converts
	// the list of videos to JSON because of the @ResponseBody
	// annotation.
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList(){
		return Lists.newArrayList(videos.findAll());
	}
	
	// Receives POST requests to /video and converts the HTTP
	// request body, which should contain json, into a Video
	// object before adding it to the list. The @RequestBody
	// annotation on the Video parameter is what tells Spring
	// to interpret the HTTP request body as JSON and convert
	// it into a Video object to pass into the method. The
	// @ResponseBody annotation tells Spring to conver the
	// return value from the method back into JSON and put
	// it into the body of the HTTP response to the client.
	//
	// The VIDEO_SVC_PATH is set to "/video" in the VideoSvcApi
	// interface. We use this constant to ensure that the 
	// client and service paths for the VideoSvc are always
	// in synch.
	//
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v){
		videos.save(v);
		return v;
	}
	
	// Returns the video for with the given
	// identifier.
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH +"/{id}", method=RequestMethod.GET)
	public @ResponseBody Video getVideo(
			@PathVariable("id") final Long id,
			HttpServletResponse response){
		Video video = videos.findOne(id);
		
		if(video == null){	
			response.setStatus(404);
		}
		return video;
	}

	// Allows a user to like a video. Returns 200 Ok on success, 404 if the
    // video is not found, or 400 if the user has already liked the video.
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH +"/{id}/like", method=RequestMethod.POST)
	public @ResponseBody Video likeVideo(
			@PathVariable("id") final Long id,
			HttpServletResponse response,
			OAuth2Authentication auth){
		Video video = videos.findOne(id);
		
		if(video == null){	
			response.setStatus(404);
		} else {
			User activeUser = (User) auth.getPrincipal();
			if(video.likedByList.contains(activeUser.getUsername())){
				response.setStatus(400);
			} else {
				video.setLikes(video.getLikes()+ 1);
				video.likedByList.add(activeUser.getUsername());
				videos.save(video);
			}
		}
		return video;
	}
	
	// Allows a user to unlike a video that he/she previously liked. Returns 200 OK
    // on success, 404 if the video is not found, and a 400 if the user has not 
    // previously liked the specified video.
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH +"/{id}/unlike", method=RequestMethod.POST)
	public @ResponseBody Video unlikeVideo(
			@PathVariable("id") final Long id,
			HttpServletResponse response,
			OAuth2Authentication auth){
		Video video = videos.findOne(id);
		
		if(video == null){	
			response.setStatus(404);
		} else {
			User activeUser = (User) auth.getPrincipal();
			if(!video.likedByList.contains(activeUser.getUsername())){
				response.setStatus(400);
			} else {
				video.setLikes(video.getLikes()- 1);
				video.likedByList.remove(activeUser.getUsername());
				videos.save(video);
			}
		}
		return video;
	}
	
	// Returns a list of the string usernames of the users that have liked the specified
    // video. If the video is not found, a 404 error should be generated.
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH +"/{id}/likedby", method=RequestMethod.GET)
	public @ResponseBody Collection<String> likedBy(
			@PathVariable("id") final Long id,
			HttpServletResponse response){
		Video video = videos.findOne(id);
		
		Set<String> likedBy = null;
		if(video == null){	
			response.setStatus(404);
		} else {
			likedBy = video.likedByList;
		}
		return likedBy;
	}
}
