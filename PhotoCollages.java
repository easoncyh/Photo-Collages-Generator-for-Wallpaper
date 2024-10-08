import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import java.util.Random;
import java.util.ArrayList;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.util.Iterator;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.FileImageInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

class PhotoCollages {
	public static int wallpaperWidth = 1920;
	public static int wallpaperHeight = 1080;
	public static int marginBorderSize = 50;
	public static int gridSize = 200;
	public static int borderSize = 20;
	public static int minWdithAndHeight = gridSize-(borderSize*2);
	
    public static void main(String[] args) {
        System.out.println("Hello, World!");

		gridSize = getOptimalGridSize();
		minWdithAndHeight = gridSize-(borderSize*2);
		
		long startTime = System.currentTimeMillis();
		String wallpaperFileName = "output_"+wallpaperWidth+"x"+wallpaperHeight+"_"+startTime;
		PhotoPicker picker = new PhotoPicker("src");
		Wallpaper w = new Wallpaper(wallpaperFileName, wallpaperWidth, wallpaperHeight);
		w.drawAndSaveAllPhotos(picker);
		w.save();
		
		long durationInSeconds = (System.currentTimeMillis()-startTime)/1000;
		System.out.println("Completed generating the wallpaper in " + durationInSeconds + " seconds.");
    }

	public static int getOptimalGridSize() {
		int maxGridSize = wallpaperHeight > wallpaperWidth ? wallpaperWidth/2 : wallpaperHeight/2;
		int minSumofHorizontalGapAndVerticalGap = wallpaperWidth > wallpaperHeight ? wallpaperWidth : wallpaperHeight;
		int initGridSize = 200;
		int optimalGridSize = initGridSize;

		int usableWidth = wallpaperWidth-(marginBorderSize*2);
		int usableHeight = wallpaperHeight-(marginBorderSize*2);
		int gapSum;
		int gapMin = 5;

		System.out.println("initGridSize: " + initGridSize);
		System.out.println("maxGridSize: " + maxGridSize);

		for(int i=initGridSize; i<maxGridSize; i++) {
			if(((usableWidth/i) >= 4) && ((usableHeight/i) >=4)) {
				gapSum = usableWidth%i + usableHeight%i;
				if(gapSum >= gapMin*2 && gapSum < minSumofHorizontalGapAndVerticalGap) {
					minSumofHorizontalGapAndVerticalGap = gapSum;
					optimalGridSize = i;
					System.out.println("\tFound better grid size: " + i + " with gap sum of " + gapSum);
				}
			}
		}
		System.out.println("Final gridSize: " + optimalGridSize);

		return optimalGridSize;
	}
}

class PhotoPicker {
	String folderPath;
	ArrayList<PhotoGrid> photoGridArrayList;
	ArrayList<PhotoGrid> suitablePhotos;
	ArrayList<PhotoGrid> suitableNewPhotos;
	Random randNumGenerator;
	
	public PhotoPicker(String inFolderPath) {
		folderPath = inFolderPath;
		photoGridArrayList = new ArrayList<PhotoGrid>();
		randNumGenerator = new Random(System.currentTimeMillis());
		readSourcePhotos();
	}
	
	public void readSourcePhotos() {
		List<Path> imageFilePaths = null;
		
		try {
			imageFilePaths = listAllFilesInDir(Paths.get(folderPath));
		} catch (IOException e) {
			System.out.println("Error reading: " + e);
		}
 		
		if(imageFilePaths.size() > 0) {
			System.out.println("Total no. of image files found: " + imageFilePaths.size());
			
			/*
			imageFilePaths.forEach(p -> {
				System.out.println(p.getFileName());
			});
			*/
		} else {
			System.out.println("No image file found in the directory: " + imageFilePaths.size());
		}
		
		System.out.println("fileList: ");
		PhotoGrid tempPhotoGrid;
		
		for(int i = 0; i < imageFilePaths.size(); i++) {
			if(isImageFile(imageFilePaths.get(i).toFile())) {
				tempPhotoGrid = new PhotoGrid(imageFilePaths.get(i).toString());
				
				if(tempPhotoGrid.isLargerThanMinimumDimension()) {
					System.out.println("\t+++Selected: " + imageFilePaths.get(i).getFileName());
					photoGridArrayList.add(tempPhotoGrid);
				} else {
					System.out.println("\t---Rejected: " + imageFilePaths.get(i).getFileName());
				}
			}
		}
	}
	
	public List<Path> listAllFilesInDir(Path path) throws IOException {
		List<Path> files;

		try (Stream<Path> walk = Files.walk(path)) {
			files = walk.filter(Files::isRegularFile).filter(p -> isImageFile(p.toFile()))
					.collect(Collectors.toList());
		}
		
		return files;
	}


	public boolean isImageFile(File f) {
		return f.isFile() && (f.getName().toLowerCase().endsWith(".jpg"));
	}
	
	public BufferedImage selectPhotoImage(int inWidth, int inHeight) {
		// select photos meeting the dimension requirement
		
		suitablePhotos = new ArrayList<PhotoGrid>();
		suitableNewPhotos = new ArrayList<PhotoGrid>();
		
		for(int i=0; i < photoGridArrayList.size(); i++) {
			if(photoGridArrayList.get(i).isLargerThanDimension(inWidth, inHeight)) {
				suitablePhotos.add(photoGridArrayList.get(i));
				
				if(!photoGridArrayList.get(i).hasSelected) {
					suitableNewPhotos.add(photoGridArrayList.get(i));
				}
			}
		}
		
		//randomly select a photo from the photo array
		int rndNum;
		
		if(suitableNewPhotos.size() > 0) {
			// when there are still new photos
			rndNum = randNumGenerator.nextInt(suitableNewPhotos.size());
			System.out.println("\tSelected image: " + suitableNewPhotos.get(rndNum).inputFilename);
			return suitableNewPhotos.get(rndNum).getBufferedImage(inWidth, inHeight);
		} else {
			// when all photos are selected
			rndNum = randNumGenerator.nextInt(suitablePhotos.size());
			System.out.println("\tSelected image: " + suitablePhotos.get(rndNum).inputFilename);
			return suitablePhotos.get(rndNum).getBufferedImage(inWidth, inHeight);
		}
	}
}

class PhotoGrid {
	public BufferedImage originalImage;
	public int width, height;
	public String inputFilename;
	public boolean hasSelected = false;
	public int minWdithAndHeight;
	
	public PhotoGrid(String inFilename) {
		inputFilename = inFilename;
		minWdithAndHeight = PhotoCollages.minWdithAndHeight;
		//getWidthAndHeightFromImage();
		
		// good performance method to get image dimension with reference to this Stackoverflow post
		// https://blog.csdn.net/10km/article/details/52119508
		try {
			getImageDimension();
		} catch (IOException e) {
		}
	}
	
	public boolean isLargerThanMinimumDimension() {
		return isLargerThanDimension(minWdithAndHeight, minWdithAndHeight);
	}
	
	public boolean isLargerThanDimension(int inWidth, int inHeight) {
		return (width >= inWidth) && (height >= inHeight);
	}
	
	public BufferedImage convertToBufferedImage(Image img) {
		if (img instanceof BufferedImage)
			{
				return (BufferedImage) img;
			}

			// Create a buffered image with transparency
			BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

			// Draw the image on to the buffered image
			Graphics2D bGr = bimage.createGraphics();
			bGr.drawImage(img, 0, 0, null);
			bGr.dispose();

			// Return the buffered image
			return bimage;
	}
	
	/*
	public BufferedImage cropAndResizeImage(int dstWidth, int dstHeight) {
		// The size of one photo grid is PhotoCollages.minWdithAndHeight.  
		// The actual image size of one photo grid is:
		//     PhotoCollages.minWdithAndHeight - PhotoCollages.borderSize*2
		// For the position of the image is:
		//     posX = pos-x of the photoGrid+PhotoCollages.borderSize
		//     posY = pos-y of the photoGrid+PhotoCollages.borderSize
		
		// resize to fit the short
		BufferedImage scaledImage;
		int cropPosX = 0;
		int cropPosY = 0;
		
		float widthMagnification = ((float)width / (float)dstWidth);
		float heightMagnification = ((float)height / (float)dstHeight);
		int estHeightAfterCropping = Math.round(dstHeight * widthMagnification);
		int estWidthAfterCropping = Math.round(dstWidth * heightMagnification);
		int widthAfterCropping, heightAfterCropping;

		System.out.println("width: " + width);
		System.out.println("height: " + height);
		System.out.println("estWidthAfterCropping: " + estWidthAfterCropping);
		System.out.println("estHeightAfterCropping: " + estHeightAfterCropping);

		if(height >= estHeightAfterCropping) {
			widthAfterCropping = width;
			heightAfterCropping = estHeightAfterCropping;
			cropPosY = (height-estHeightAfterCropping)/2;
		} else {
			widthAfterCropping = estWidthAfterCropping;
			heightAfterCropping = height;
			cropPosX = (width-estWidthAfterCropping)/2;
		}

		System.out.println("widthAfterCropping: " + widthAfterCropping);
		System.out.println("heightAfterCropping: " + heightAfterCropping);
		System.out.println("cropPosX: " + cropPosX);
		System.out.println("cropPosY: " + cropPosY);

		BufferedImage croppedImage = originalImage.getSubimage(cropPosX, cropPosY, widthAfterCropping, heightAfterCropping);
		BufferedImage resizedImage = new BufferedImage(dstWidth, dstHeight, BufferedImage.TYPE_INT_RGB);

		Graphics2D graphics2D = resizedImage.createGraphics();
		graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		graphics2D.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);

		graphics2D.drawImage(croppedImage, 0, 0, dstWidth, dstHeight, null);
		graphics2D.dispose();
		return resizedImage;
	}
	 */

	/* 
	 public BufferedImage cropAndResizeImage(int dstWidth, int dstHeight) {
		// The size of one photo grid is PhotoCollages.minWdithAndHeight.  
		// The actual image size of one photo grid is:
		//     PhotoCollages.minWdithAndHeight - PhotoCollages.borderSize*2
		// For the position of the image is:
		//     posX = pos-x of the photoGrid+PhotoCollages.borderSize
		//     posY = pos-y of the photoGrid+PhotoCollages.borderSize
		
		
		int cropPosX = 0;
		int cropPosY = 0;
		float widthMagnification = ((float)dstWidth / (float)width);
		float heightMagnification = ((float)dstHeight / (float)height);
		int estHeightAfterScaling = Math.round(height * widthMagnification);
		int estWidthAfterScaling = Math.round(width * heightMagnification);
		int widthAfterScaling, heightAfterScaling;

		if(estHeightAfterScaling >= dstHeight) {
			widthAfterScaling = dstWidth;
			heightAfterScaling = estHeightAfterScaling;
			cropPosY = (estHeightAfterScaling-dstHeight)/2;
		} else {
			widthAfterScaling = estWidthAfterScaling;
			heightAfterScaling = height;
			cropPosX = (estWidthAfterScaling-dstWidth)/2;
		}

		//BufferedImage croppedImage = originalImage.getSubimage(cropPosX, cropPosY, widthAfterCropping, heightAfterCropping);
		BufferedImage resizedImage = new BufferedImage(widthAfterScaling, heightAfterScaling, BufferedImage.TYPE_INT_RGB);

		Graphics2D graphics2D = resizedImage.createGraphics();
		//graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		//graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		//graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		//graphics2D.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
		graphics2D.drawImage(originalImage, 0, 0, widthAfterScaling, heightAfterScaling, null);
		graphics2D.dispose();

		return resizedImage.getSubimage(cropPosX, cropPosY, dstWidth, dstHeight);
	}
	*/

	public BufferedImage scaleAndCropImage(int dstWidth, int dstHeight) {
		// The size of one photo grid is 250px.  
		// It is the same value as PhotoCollages.minWdithAndHeight.
		// The actual image size of one photo grid is:
		//     PhotoCollages.minWdithAndHeight - PhotoCollages.borderSize*2
		//     250px - 5px*2
		// For the position of the image is:
		//     posX = pos-x of the photoGrid+PhotoCollages.borderSize
		//     posY = pos-y of the photoGrid+PhotoCollages.borderSize
		
		// resize to fit the short
		BufferedImage scaledImage;
		int cropPosX = 0;
		int cropPosY = 0;
		
		float widthMagnification = ((float)dstWidth / (float)width);
		int heightAfterScale = Math.round(height * widthMagnification);
		
		if(heightAfterScale >= dstHeight) {
			scaledImage = convertToBufferedImage(originalImage.getScaledInstance(dstWidth, -1, Image.SCALE_AREA_AVERAGING));
			cropPosY = (scaledImage.getHeight()-dstHeight)/2;
		} else {
			scaledImage = convertToBufferedImage(originalImage.getScaledInstance(-1, dstHeight, Image.SCALE_AREA_AVERAGING));
			cropPosX = (scaledImage.getWidth()-dstWidth)/2;
		}
		
		//System.out.println("\tscaledImage width: "+scaledImage.getWidth());
		//System.out.println("\tscaledImage height: "+scaledImage.getHeight());
		
		return scaledImage.getSubimage(cropPosX, cropPosY, dstWidth, dstHeight);
	}
	
	public void getWidthAndHeightFromImage() {
		// set width and height
		readImage();
		
		// free variable otherwise there will be memory leakage
		originalImage = null;
	}
	
	public void getImageDimension() throws IOException {
		File imgFile = new File(inputFilename);
		
		int pos = imgFile.getName().lastIndexOf(".");
		if (pos == -1) {
			throw new IOException("No extension for file: " + (imgFile).getAbsolutePath());
		}
		
		String suffix = imgFile.getName().substring(pos + 1);
		Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(suffix);
		while(iter.hasNext()) {
			ImageReader reader = iter.next();
			try {
				ImageInputStream stream = new FileImageInputStream(imgFile);
				reader.setInput(stream);
				width = reader.getWidth(reader.getMinIndex());
				height = reader.getHeight(reader.getMinIndex());
			} catch (IOException e) {
				System.out.println("Error reading: " + e);
			} finally {
			reader.dispose();
			}
		}

		throw new IOException("Not a known image file: " + imgFile.getAbsolutePath());
	}
	
	public void readImage() {
		if(originalImage == null) {
			try {
			   originalImage = ImageIO.read(new File(inputFilename));
			} catch (IOException e) {
			}
			
			width = originalImage.getWidth();
			height = originalImage.getHeight();
		}
	}
	
	public BufferedImage getBufferedImage(int inWidth, int inHeight) {
		hasSelected = true;
		
		readImage();
		BufferedImage image = scaleAndCropImage(inWidth, inHeight);
		// free the originalImage after use, otherwise, it will cause memory leak
		originalImage = null;
		
		return image;
	}
}

class PositionAndDimension {
	public int posX, posY, width, height;
	public int minWdithAndHeight, gridSize, borderSize, marginBorderSize;
	//mapIndexI, mapIndexJ, blockSize, minWdithAndHeight

	public PositionAndDimension(int inIndexI,
								int inIndexJ,
								int inHorizontalGap,
								int inVerticalGap,
								int inBlockSize) {
		
		borderSize = PhotoCollages.borderSize;
		marginBorderSize = PhotoCollages.marginBorderSize;
		gridSize = PhotoCollages.gridSize;
		minWdithAndHeight = PhotoCollages.minWdithAndHeight;

		posX = marginBorderSize + inIndexJ * (gridSize + inHorizontalGap) + borderSize;
		posY = marginBorderSize + inIndexI * (gridSize + inVerticalGap) + borderSize;
		width = inBlockSize*gridSize+(inBlockSize-1)*inHorizontalGap-2*borderSize;
		height = inBlockSize*gridSize+(inBlockSize-1)*inVerticalGap-2*borderSize;
	}
}

class GridMap {
	public ArrayList<PositionAndDimension> posAndDim = new ArrayList<PositionAndDimension>();
	int width, height, minWdithAndHeight, gridSize, borderSize, marginBorderSize;
	//public int[] gridPosXArray, gridPosYArray;
	// all grids are square, 1x1, 2x2, 3x3, etc
	public int largestGridSize;
	public int photoNumPerRow, horizontalGap, photoNumPerColumn, verticalGap;
	public int[][] map;

	Random randNumGenerator = new Random();

	public GridMap(int inWidth, int inHeight) {
		width = inWidth;
		height = inHeight;
		gridSize = PhotoCollages.gridSize;
		minWdithAndHeight = PhotoCollages.minWdithAndHeight;
		borderSize = PhotoCollages.borderSize;
		marginBorderSize = PhotoCollages.marginBorderSize;
	}

	public void generateLayout() {
		calculateGridPosXAndPosY();

		map = new int[photoNumPerColumn][];
		for(int i=0; i < map.length; i++) {
			map[i] = new int[photoNumPerRow];

			for(int j=0; j < map[i].length; j++) {
				map[i][j] = 0;
			}
		}

		/*
		for(int i = 0; i < map.length; i++) {
			for(int j = 0; j < map[i].length; j++) {
				System.out.println("["+i+", "+j+"] with value of "+map[i][j]);
			}
		}
		 */

		
		//printMap();
		
		int tempBlockSize;
		int photoCount = 1;

		for(int i = 0; i < map.length; i++) {
			for(int j = 0; j < map[i].length; j++) {
				if(map[i][j] == 0) { // this grid is not occupied
					System.out.println("Travel at grid ("+i+", "+j+")");

					do {
						tempBlockSize = getRandomSizeBlock();
					} while (hasInsufficientSpace(i, j, tempBlockSize));
					
					System.out.println("\tBingo!");
					allocateBlock(i, j, tempBlockSize, photoCount);
					photoCount += 1;
					
				}
			}
		}
		
		printMap();
	}

	public boolean hasInsufficientSpace(int mapIndexI, int mapIndexJ, int inBlockSize) {
		//boolean insufficientSpace = true;
		boolean isFree = true;

		for (int a=0; a < inBlockSize; a++) {
			for(int b=0; b < inBlockSize; b++) {
				//System.out.println("\tgoing to check ["+(mapIndexI+a)+", "+(mapIndexJ+b)+"]");
				if (((mapIndexI+a) <= photoNumPerColumn-1) && ((mapIndexJ+b) <= photoNumPerRow-1)) { // the checking blocks are still within the map size
					//System.out.println("\tChecking at ["+(mapIndexI+a)+", "+(mapIndexJ+b)+"] and the value is "+map[mapIndexI+a][mapIndexJ+b]);
					isFree = isFree && (map[mapIndexI+a][mapIndexJ+b] == 0);
				} else {
					isFree = false;
				}
			}
		}

		return !isFree;
	}

	public void allocateBlock(int mapIndexI, int mapIndexJ, int blockSize, int photoCount) {
		for (int a=0; a < (blockSize); a++) {
			for(int b=0; b < (blockSize); b++) {
				//System.out.println("Mark ["+(mapIndexI+a)+", "+(mapIndexJ+b)+"] with value of "+photoCount);
				map[mapIndexI+a][mapIndexJ+b] = photoCount;
			}
		}

		posAndDim.add(new PositionAndDimension(
			mapIndexI, mapIndexJ, 
			horizontalGap, verticalGap,
			blockSize
		));
	}

	public void printMap() {
		System.out.println("-------- start of print map -------------");
		for(int i = 0; i < map.length; i++) {
			for(int j = 0; j < map[i].length; j++) {
				System.out.print("[");
				System.out.printf("%02d", map[i][j]);
				System.out.print("]");
			}
			System.out.println("");
		}
		System.out.println("-------- end of print map -------------");
	}

	public int getRandomSizeBlock() {
		int rndNum = randNumGenerator.nextInt(largestGridSize)+1;
		System.out.println("\tTry random block size: " + rndNum);
		return rndNum;
	}

	public void calculateGridPosXAndPosY() {
		System.out.println("====================================================================");
		
		int usableWidth = width-(marginBorderSize*2);
		photoNumPerRow = usableWidth/gridSize;
		int sumofHorizontalGap = usableWidth%gridSize;
		horizontalGap = sumofHorizontalGap / (photoNumPerRow - 1);
		
		/*
		gridPosXArray = new int[photoNumPerRow];
		
		for(int i = 0; i < photoNumPerRow; i++) {
			gridPosXArray[i] = marginBorderSize + (i * (gridSize+horizontalGap));
		}
		*/

		System.out.println("usableWidth: " + usableWidth);
		System.out.println("photoNumPerRow: " + photoNumPerRow);
		System.out.println("sumofHorizontalGap: " + sumofHorizontalGap);
		System.out.println("Horizontal gap is " + horizontalGap);
		
		int usableHeight = height-(marginBorderSize*2);
		photoNumPerColumn = usableHeight/gridSize;
		int sumofVerticalGap = usableHeight%gridSize;
		verticalGap = sumofVerticalGap / (photoNumPerColumn - 1);
		
		/* 
		gridPosYArray = new int[photoNumPerColumn];
		
		for(int j = 0; j < photoNumPerColumn; j++) {
			gridPosYArray[j] = marginBorderSize + (j * (gridSize+verticalGap));
		}
		*/

		System.out.println("usableHeight: " + usableHeight);
		System.out.println("photoNumPerColumn: " + photoNumPerColumn);
		System.out.println("sumofVerticalGap: " + sumofVerticalGap);
		System.out.println("Vertical gap is " + verticalGap);

		if(photoNumPerRow <= photoNumPerColumn) {
			if(photoNumPerRow <= 3) {
				largestGridSize = 2;
			} else {
				largestGridSize = photoNumPerRow/2;
			}
		} else {
			if(photoNumPerColumn <= 3) {
				largestGridSize = 2;
			} else {
				largestGridSize = photoNumPerColumn/2;
			}
		}
	}
}

class Wallpaper {
	public String outputPath;
	public String outputFilename;
	public BufferedImage outputImage = null;
	public int width, height;
	public Graphics g;
	public Graphics2D g2d;
	public int minWdithAndHeight;
	public int borderSize;
	public GridMap gridMap;

	public Wallpaper(String inFilename, int inWidth, int inHeight) {
		outputFilename = inFilename;
		width = inWidth;
		height = inHeight;
		minWdithAndHeight = PhotoCollages.minWdithAndHeight;
		borderSize = PhotoCollages.borderSize;
		
		gridMap = new GridMap(width, height);
		gridMap.generateLayout();

		outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		g2d = outputImage.createGraphics();
		g2d.setPaint (new Color (0, 0, 0));
		g2d.fillRect (0, 0, width, height);
		g2d.dispose();
		
		// create output folder
		outputPath = "./output/";
		new File(outputPath).mkdirs();
	}
	
	public void drawPhotoGrid(PositionAndDimension posAndDim) {
		g2d = outputImage.createGraphics();
		g2d.setPaint(new Color (255, 255, 255));

		System.out.println("Draw ["+(posAndDim.posX)+", "+(posAndDim.posY)+"] with width of "+posAndDim.width + ", " + posAndDim.height);

		g2d.fillRect(posAndDim.posX, posAndDim.posY, posAndDim.width, posAndDim.height);
		g2d.dispose();
	}
	
	public void drawPhoto(PositionAndDimension posAndDim, BufferedImage bi) {
		g = outputImage.getGraphics();
		g.drawImage(bi, posAndDim.posX, posAndDim.posY, null);
		//g.drawImage(bi, posAndDim.posX+PhotoCollages.marginBorderSize, posAndDim.posY+PhotoCollages.marginBorderSize, null);
		g.dispose();
	}
	
	public void save() {
		File outputFile = new File(outputPath+outputFilename+".jpg");
		
		try {
           //ImageIO.write(outputImage, "jpg", outputFile);
			ImageOutputStream outStream =  ImageIO.createImageOutputStream(outputFile);
			Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpeg");
			ImageWriter writer = iter.next();
			ImageWriteParam writeParam = writer.getDefaultWriteParam();
			writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			writeParam.setCompressionQuality(0.9f);
			writer.setOutput(outStream);
			writer.write(null, new IIOImage(outputImage,null,null),writeParam);
			writer.dispose();
		} catch (IOException e) {
		}
	}
	
	public void drawAndSaveAllPhotos(PhotoPicker picker) {
		PositionAndDimension tempPosAndDim;
		
		for(int i=0; i < gridMap.posAndDim.size(); i++) {
			tempPosAndDim = gridMap.posAndDim.get(i);
			
			System.out.println("At ("+gridMap.posAndDim.get(i).posX + "," +gridMap.posAndDim.get(i).posY + "), "+
							"draw an image with dimension "+tempPosAndDim.width+"x"+tempPosAndDim.height);

			drawPhoto(tempPosAndDim, picker.selectPhotoImage(tempPosAndDim.width, tempPosAndDim.height));
		}
		
		save();
	}
}

