import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import javax.swing.*;
import java.util.Random;
import java.util.ArrayList;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.util.Date;

class PhotoCollages {
    public static void main(String[] args) {
        System.out.println("Hello, World!"); 
		
		long timestamp = (new Date()).getTime();
		int wallpaperWidth = 1920;
		int wallpaperHeight = 1080;
		String wallpaperFileName = "output_"+wallpaperWidth+"x"+wallpaperHeight+"_"+timestamp;
		Wallpaper w = new Wallpaper(wallpaperFileName, wallpaperWidth, wallpaperHeight);

		ArrayList<PhotoGrid> photoGridArrayList = new ArrayList<PhotoGrid>();
		readSourcePhotos(photoGridArrayList, "src");
		
		GridMap map = new GridMap(1920, 1080, 240, 5);
		map.generateLayout();

		for(int i=0; i < map.posAndDim.size(); i++) {
			//w.drawPhotoGrid(map.posAndDim.get(i));
			w.drawPhoto(selectPhoto(photoGridArrayList), w.gridPosXArray[i], w.gridPosYArray[j]);
		}
		
		w.save();
    }
	
	public static void readSourcePhotos(ArrayList<PhotoGrid> inArrayList, String dir) {
		
		File[] fileList = new File(dir).listFiles();
		
		System.out.println("fileList: ");
				
		for(int i = 0; i < fileList.length; i++) {
			if(fileList[i].isFile() && (fileList[i].getName().toLowerCase().endsWith(".jpg"))) {
				System.out.println("\t" + fileList[i].getPath());
				inArrayList.add(new PhotoGrid(fileList[i].getPath()));
			}
		}
	}
	
	public static BufferedImage selectPhoto(ArrayList<PhotoGrid> inPhotoList) {
		//randomly select a photo from the photo array

		Random randNumGenerator = new Random();
		ArrayList<PhotoGrid> newPhotoList = new ArrayList<PhotoGrid>();
		
		for(int i = 0; i < inPhotoList.size(); i++) {
			if(!inPhotoList.get(i).hasSelected) {
				newPhotoList.add(inPhotoList.get(i));
			}
		}
		
		int rndNum;

		if(newPhotoList.size() > 0) {
			// when there are still new photos
			rndNum = randNumGenerator.nextInt(newPhotoList.size());
			System.out.println("Selected image: " + newPhotoList.get(rndNum).inputFilename);
			return newPhotoList.get(rndNum).getBufferedImage();
		} else {
			// when all photos are selected
			rndNum = randNumGenerator.nextInt(inPhotoList.size());
			System.out.println("Selected image: " + inPhotoList.get(rndNum).inputFilename);
			return inPhotoList.get(rndNum).getBufferedImage();
		}	
	}
}

class PhotoGrid {
	public BufferedImage originalImage;
	public BufferedImage image;
	public int width, height;
	public int minWdithAndHeight = 240;
	public String inputFilename;
	public boolean hasSelected = false;
	
	public PhotoGrid(String inFilename) {
		inputFilename = inFilename;
	}
	
	//TODO: pass in parameter to check the width and height
	public boolean sizeIsLargerThanRequirement() {
		return (originalImage.getWidth() >= minWdithAndHeight) && (originalImage.getHeight() >= minWdithAndHeight);
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
	
	//TODO: pass in the width and height to scale and crop 
	public BufferedImage scaleAndCropImage() {
		if (!sizeIsLargerThanRequirement()) {
			return null;
		}

		// resize to fit the short
		BufferedImage scaledImage;
		int cropPosX = 0;
		int cropPosY = 0;
		
		if(originalImage.getWidth() >= originalImage.getHeight()) {
			scaledImage = convertToBufferedImage(originalImage.getScaledInstance(-1, minWdithAndHeight, Image.SCALE_AREA_AVERAGING));
			cropPosX = (scaledImage.getWidth()-minWdithAndHeight)/2;
		} else {
			scaledImage = convertToBufferedImage(originalImage.getScaledInstance(minWdithAndHeight, -1, Image.SCALE_AREA_AVERAGING));
			cropPosY = (scaledImage.getHeight()-minWdithAndHeight)/2;
		}
		
		return scaledImage.getSubimage(cropPosX, cropPosY, minWdithAndHeight, minWdithAndHeight);
	}
	
	public BufferedImage getBufferedImage() {
		hasSelected = true;
		
		if(image == null) {
			try {
			   originalImage = ImageIO.read(new File(inputFilename));
			} catch (IOException e) {
			}
			
			width = originalImage.getWidth();
			height = originalImage.getHeight();
			
			image = scaleAndCropImage();
		}
		
		// free the originalImage after use, otherwise, it will cause memory leak
		originalImage = null;
		
		return image;
	}
}

class PositionAndDimension {
	public int posX, posY, width, height;

	//mapIndexI, mapIndexJ, blockSize, minWdithAndHeight

	public PositionAndDimension(int inIndexI,
								int inIndexJ,
								int inHorizontalGap,
								int inVerticalGap,
								int inMinWdithAndHeight,
								int inBlockSize) {
		posX = inIndexJ *  (inMinWdithAndHeight + inHorizontalGap);
		posY = inIndexI *  (inMinWdithAndHeight + inVerticalGap);
		width = inBlockSize*inMinWdithAndHeight + inHorizontalGap*(inBlockSize-1);
		height = inBlockSize*inMinWdithAndHeight + inVerticalGap*(inBlockSize-1);

		System.out.println("[PositionAndDimension] width: " + width);
		System.out.println("[PositionAndDimension] height: " + height);
	}
}

class GridMap {
	public ArrayList<PositionAndDimension> posAndDim = new ArrayList<PositionAndDimension>();
	int width, height, minWdithAndHeight, borderSize;
	public int[] gridPosXArray, gridPosYArray;
	// all grids are square, 1x1, 2x2, 3x3, etc
	public int largestGridSize;
	public int photoNumPerRow, horizontalGap, photoNumPerColumn, verticalGap;
	public int[][] map;

	Random randNumGenerator = new Random();

	public GridMap(int inWidth, int inHeight, int inMinWdithAndHeight, int inBorderSize) {
		width = inWidth;
		height = inHeight;
		minWdithAndHeight = inMinWdithAndHeight;
		borderSize = inBorderSize;
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

		
		printMap();
		
		int tempBlockSize;
		int photoCount = 1;

		for(int i = 0; i < map.length; i++) {
			for(int j = 0; j < map[i].length; j++) {
				if(map[i][j] == 0) { // this grid is not occupied
					System.out.println("Arrive at grid: "+i+", "+j+"]");

					do {
						tempBlockSize = getRandomSizeBlock();
					} while (hasInsufficientSpace(i, j, tempBlockSize));
					
					allocateBlock(i, j, tempBlockSize, photoCount);
					photoCount += 1;

					printMap();
				}
			}
		}

	}

	public boolean hasInsufficientSpace(int mapIndexI, int mapIndexJ, int inBlockSize) {
		//boolean insufficientSpace = true;
		boolean isFree = true;

		for (int a=0; a < inBlockSize; a++) {
			for(int b=0; b < inBlockSize; b++) {
				System.out.println("\tgoing to check ["+(mapIndexI+a)+", "+(mapIndexJ+b)+"]");
				if (((mapIndexI+a) <= photoNumPerColumn-1) && ((mapIndexJ+b) <= photoNumPerRow-1)) { // the checking blocks are still within the map size
					System.out.println("\tChecking at ["+(mapIndexI+a)+", "+(mapIndexJ+b)+"] and the value is "+map[mapIndexI+a][mapIndexJ+b]);
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
				System.out.println("Mark ["+(mapIndexI+a)+", "+(mapIndexJ+b)+"] with value of "+photoCount);
				map[mapIndexI+a][mapIndexJ+b] = photoCount;
			}
		}

		posAndDim.add(new PositionAndDimension(
			mapIndexI, mapIndexJ, 
			horizontalGap, verticalGap,
			minWdithAndHeight, blockSize
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
		System.out.println("Random block size: " + rndNum);
		return rndNum;
	}

	public void calculateGridPosXAndPosY() {
		System.out.println("====================================================================");
		
		int usableWidth = width-(borderSize*2);
		photoNumPerRow = usableWidth/minWdithAndHeight;
		int sumofHorizontalGap = usableWidth%minWdithAndHeight;
		horizontalGap = sumofHorizontalGap / (photoNumPerRow - 1);
		
		gridPosXArray = new int[photoNumPerRow];
		
		for(int i = 0; i < photoNumPerRow; i++) {
			gridPosXArray[i] = borderSize + (i * (minWdithAndHeight+horizontalGap));
		}
		
		System.out.println("usableWidth: " + usableWidth);
		System.out.println("photoNumPerRow: " + photoNumPerRow);
		System.out.println("sumofHorizontalGap: " + sumofHorizontalGap);
		System.out.println("Horizontal gap is " + horizontalGap);
		
		int usableHeight = height-(borderSize*2);
		photoNumPerColumn = usableHeight/minWdithAndHeight;
		int sumofVerticalGap = usableHeight%minWdithAndHeight;
		verticalGap = sumofVerticalGap / (photoNumPerColumn - 1);
		
		gridPosYArray = new int[photoNumPerColumn];
		
		for(int j = 0; j < photoNumPerColumn; j++) {
			gridPosYArray[j] = borderSize + (j * (minWdithAndHeight+verticalGap));
		}
		
		System.out.println("usableHeight: " + usableHeight);
		System.out.println("photoNumPerColumn: " + photoNumPerColumn);
		System.out.println("sumofVerticalGap: " + sumofVerticalGap);
		System.out.println("Vertical gap is " + verticalGap);

		if(photoNumPerRow <= photoNumPerColumn) {
			largestGridSize = photoNumPerRow - 1;
		} else {
			largestGridSize = photoNumPerColumn - 1;
		}
	}
}

class Wallpaper {
	public String outputFilename;
	public BufferedImage outputImage = null;
	public int width, height;
	public Graphics g;
	public Graphics2D g2d;
	public int minWdithAndHeight = 250;
	public int borderSize = 5;
	public int[] gridPosXArray;
	public int[] gridPosYArray;
	public GridMap gridMap;

	public Wallpaper(String inFilename, int inWidth, int inHeight) {
		outputFilename = inFilename;
		width = inWidth;
		height = inHeight;
		
		gridMap = new GridMap(width, height, minWdithAndHeight, borderSize);
		gridMap.generateLayout();

		outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		g2d = outputImage.createGraphics();
		g2d.setPaint (new Color (0, 0, 0));
		g2d.fillRect (0, 0, width, height);
		g2d.dispose();
		
		calculateGridPosXAndPosY();
	}
	
	public void calculateGridPosXAndPosY() {
		System.out.println("====================================================================");
		
		int usableWidth = width-(borderSize*2);
		int photoNumPerRow = usableWidth/minWdithAndHeight;
		int sumofHorizontalGap = usableWidth%minWdithAndHeight;
		int horizontalGap = sumofHorizontalGap / (photoNumPerRow - 1);
		
		gridPosXArray = new int[photoNumPerRow];
		
		for(int i = 0; i < photoNumPerRow; i++) {
			gridPosXArray[i] = borderSize + (i * (minWdithAndHeight+horizontalGap));
		}
		
		System.out.println("usableWidth: " + usableWidth);
		System.out.println("photoNumPerRow: " + photoNumPerRow);
		System.out.println("sumofHorizontalGap: " + sumofHorizontalGap);
		System.out.println("Horizontal gap is " + horizontalGap);
		
		int usableHeight = height-(borderSize*2);
		int photoNumPerColumn = usableHeight/minWdithAndHeight;
		int sumofVerticalGap = usableHeight%minWdithAndHeight;
		int verticalGap = sumofVerticalGap / (photoNumPerColumn - 1);
		
		gridPosYArray = new int[photoNumPerColumn];
		
		for(int j = 0; j < photoNumPerColumn; j++) {
			gridPosYArray[j] = borderSize + (j * (minWdithAndHeight+verticalGap));
		}
		
		System.out.println("usableHeight: " + usableHeight);
		System.out.println("photoNumPerColumn: " + photoNumPerColumn);
		System.out.println("sumofVerticalGap: " + sumofVerticalGap);
		System.out.println("Vertical gap is " + verticalGap);
	}
	
	public void drawPhotoGrid(PositionAndDimension posAndDim) {
		g2d = outputImage.createGraphics();
		g2d.setPaint(new Color (255, 255, 255));

		System.out.println("Draw ["+(borderSize+posAndDim.posX)+", "+(borderSize+posAndDim.posY)+"] with width of "+posAndDim.width + ", " + posAndDim.height);

		g2d.fillRect(borderSize+posAndDim.posX, borderSize+posAndDim.posY, posAndDim.width, posAndDim.height);
		g2d.dispose();
	}
	
	public void drawPhoto(BufferedImage bi, int posX, int posY) {
		//g2d.setPaint(new Color (0, 0, 0));
		// one grid is 350px x 350px with a border of 25px
		// Therefore, the actual image size in the grip is 300px x 300px
		//g2d.fillRect(posX+borderSize, posY+borderSize, minWdithAndHeight-(borderSize*2), minWdithAndHeight-(borderSize*2));
		
		g = outputImage.getGraphics();
		g.drawImage(bi, posX+borderSize, posY+borderSize, null);
		g.dispose();
	}
	
	public void save() {
		File outputFile = new File(outputFilename+".jpg");
		
		try {
           ImageIO.write(outputImage, "jpg", outputFile);
		} catch (IOException e) {
		}
	}
}

