package de.mpi_dortmund.ij.mpitools.helicalPicker.gui;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.HashMap;

import de.mpi_dortmund.ij.mpitools.FilamentEnhancer.FilamentEnhancer;
import de.mpi_dortmund.ij.mpitools.FilamentEnhancer.FilamentEnhancerContext;
import de.mpi_dortmund.ij.mpitools.helicalPicker.FilamentDetector.DetectionThresholdRange;
import de.mpi_dortmund.ij.mpitools.helicalPicker.FilamentDetector.FilamentDetector;
import de.mpi_dortmund.ij.mpitools.helicalPicker.FilamentDetector.FilamentDetectorContext;
import de.mpi_dortmund.ij.mpitools.helicalPicker.filamentFilter.FilamentFilterContext;
import de.mpi_dortmund.ij.mpitools.helicalPicker.filamentFilter.FilamentFilter_;
import de.mpi_dortmund.ij.mpitools.helicalPicker.logger.CentralLog;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

public class PipelineRunner {
	
	ImageStack enhanced_images = null;
	HashMap<Integer, ArrayList<Polygon>> filtered_lines = null;
	HashMap<Integer, ArrayList<Polygon>> lines = null;
	public void run(ImagePlus input_images, SliceRange range, FilamentFilterContext line_filter_context, FilamentEnhancerContext enhancer_context, FilamentDetectorContext detector_context) {
		boolean update = true;
		boolean skip_line_filter = false;
		run(input_images, range, line_filter_context, enhancer_context, detector_context, update,skip_line_filter);
	}
	
	public void run(ImagePlus input_images, SliceRange slice_range, FilamentFilterContext filterContext, FilamentEnhancerContext enhancer_context, FilamentDetectorContext detector_context, boolean update, boolean skip_line_filter) {

		boolean is_single_frame = slice_range.getSliceFrom()==slice_range.getSliceTo();
		/*
		 * Enhance images
		 */
		CentralLog.getInstance().info("Start enhancement:");		
		CentralLog.getInstance().info("Enhancement parameters - Filament width: " + enhancer_context.getFilamentWidth() + " mask_width: " + enhancer_context.getMaskWidth() + " angle_step: " + enhancer_context.getAngleStep());

		ImageStack enhanced_images = null;
		
		//For preview mode: Save single slices. If parameters changes, update.
		if(this.enhanced_images == null || update==true){
			FilamentEnhancer enhancer = new FilamentEnhancer(input_images.getStack(), enhancer_context);		
			
			enhanced_images = enhancer.getEnhancedImages(slice_range);
			this.enhanced_images = enhanced_images;
		}
		else if(update==false){
			enhanced_images = this.enhanced_images;
		}
		
		//In case of single frame, save the results
		if(is_single_frame){
			this.enhanced_images = enhanced_images;
		}
		
		
		/*
		 * Detect Filaments
		 */
		CentralLog.getInstance().info("Detect");
		
		FilamentDetector fdetect = new FilamentDetector(enhanced_images, detector_context);
		lines =  fdetect.getFilaments(slice_range);
		
		
		/*
		 * Filter filaments
		 */
		CentralLog.getInstance().info("Filter");
		FilamentFilter_ lineFilter = new FilamentFilter_();
		filtered_lines = lineFilter.filterLines(lines, filterContext, input_images.getImageStack(), enhanced_images);
		
	}

	
	public HashMap<Integer, ArrayList<Polygon>> getFilteredLines(){
		return filtered_lines;
	}
	
	public HashMap<Integer, ArrayList<Polygon>> getLines(){
		return lines;
	}
	
	public ImageProcessor getEnhancedImage(){
		return this.enhanced_images.getProcessor(0);
	}

}
