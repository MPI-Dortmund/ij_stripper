package de.mpi_dortmund.ij.mpitools.FilamentEnhancer;

import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.ZProjector;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.FHT;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

public class FilamentEnhancer_ implements PlugInFilter {

	ArrayList<FHT> fftOfFilters = null;
	int filament_width;
	int mask_width;
	int angle_step;
	boolean show_mask;
	int last_fft_mask_width=0;
	int last_fft_filament_width=0;
	public int setup(String arg, ImagePlus imp) {
		GenericDialog gd = new GenericDialog("Mask creator");
		gd.addNumericField("Filament width", 16, 0);
		gd.addNumericField("Mask width", 128, 0);
		gd.addSlider("Angle step", 1, 180, 2);
		gd.addCheckbox("Show mask", false);
	//	gd.addNumericField("Blurring radius", 10, 0);
		gd.showDialog();
		
		if(gd.wasCanceled()){
			return DONE;
		}
		
		filament_width = (int) gd.getNextNumber();
		mask_width = (int )gd.getNextNumber();
		angle_step = (int) gd.getNextNumber();
		show_mask = gd.getNextBoolean();
		
		
		
	//	IJ.run(imp, "Invert", "stack");
		
		return IJ.setupDialog(imp, DOES_ALL+PARALLELIZE_STACKS);
	}
	
	public synchronized  void fillFFTFilters(int filament_width, int mask_width, int angle_step,boolean show_mask){
		if(fftOfFilters==null || (filament_width != last_fft_filament_width) || (mask_width != last_fft_mask_width)){
			
			MaskCreator_ maskCreator = new MaskCreator_();
			fftOfFilters = new ArrayList<FHT>();
			FloatProcessor fp = maskCreator.generateMask(filament_width, mask_width);
			ImageStack maskStack = new ImageStack(1024, 1024);
			maskStack.addSlice(fp);
			FHT h = new FHT(fp.duplicate());
			h.transform();
			fftOfFilters.add(h);
			
			int N = 180/angle_step;
			
			for(int i = 1; i < N; i++){
				FloatProcessor fpdub = (FloatProcessor) fp.duplicate();
				fpdub.setInterpolationMethod(FloatProcessor.BICUBIC);
				
				fpdub.rotate(i*angle_step);
				
				maskStack.addSlice(fpdub);
				h = new FHT(fpdub);
				h.transform();
				fftOfFilters.add(h);
			}
			
			if(show_mask){
				ImagePlus mask = new ImagePlus("Mask", maskStack);
				mask.show();
			}
			last_fft_filament_width = filament_width;
			last_fft_mask_width = mask_width;
		}
	}

	public void run(ImageProcessor ip) {
		enhance_filaments(ip, filament_width, mask_width, angle_step, show_mask);
	}
	
	public void enhance_filaments(ImageProcessor ip, int filament_width, int mask_width, int angle_step,boolean show_mask){
		fillFFTFilters(filament_width,mask_width,angle_step,show_mask);
		ip.invert();
		FHT h1, h2=null;
		h1 = new FHT(ip);
		h1.transform();
		ImageStack enhancedStack = new ImageStack(ip.getWidth(), ip.getHeight());
		for(int i = 0; i < fftOfFilters.size(); i++){
			h2 = fftOfFilters.get(i);
			FHT result = h1.multiply(h2);
			result.inverseTransform();
			result.swapQuadrants();
			result.resetMinAndMax();
			
			enhancedStack.addSlice(result);
		}
		
		ImagePlus imp = new ImagePlus("enhancedstack", enhancedStack);
		
	//	imp.show();
		ZProjector zproj = new ZProjector();
		zproj.setImage(imp);
		zproj.setMethod(ZProjector.MAX_METHOD);
		zproj.doProjection();
		ImagePlus maxProj = zproj.getProjection();
		//maxProj.show();
		ByteProcessor bp = maxProj.getProcessor().convertToByteProcessor();
		for(int x = 0; x < ip.getWidth(); x++){
			for(int y = 0; y < ip.getHeight(); y++){
				ip.set(x, y,bp.get(x, y));
			}
		}
	}
	
	

}
