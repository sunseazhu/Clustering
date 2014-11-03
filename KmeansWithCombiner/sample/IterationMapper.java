package sample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Mapper.Context;

public class IterationMapper extends Mapper<Object, Text, Text, Text>{
	
    public static double[][] centroidsList = new double[5][];
    private static boolean initDone = false;

	@Override
	protected void setup(Context context) throws IOException,InterruptedException {
	    super.setup(context);
        
        //read the centroids from all the files
        for(int i = 0; i < 5; i++){
            Path path = new Path("/kmeansDM/centroid/centroid_" + i + ".txt");
            FileSystem fs = FileSystem.get(new Configuration());
            BufferedReader bf = new BufferedReader(new InputStreamReader(fs.open(path)));
            String centroidsLine = bf.readLine();
            String[] arr = centroidsLine.split("\t");
            if(initDone == false){
                centroidsList = new double[5][arr.length];
                initDone = true;
            }
            for(int j = 0; j < arr.length; j++){
                centroidsList[i][j] = Double.parseDouble(arr[j].trim());
            }
            bf.close();
            //fs.close();
        }
	}

	private double calcDist(double[] centroid, double[] point) throws Exception{
        if(centroid.length != point.length){
            throw new Exception("length of centroid and point doesnt match");
        }
        double sum = 0;
        for(int i = 0; i < centroid.length; i++){
            sum += ((centroid[i] - point[i]) * (centroid[i] - point[i]));
        }
        return Math.sqrt(sum);
    }


    public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
        String[] feature = value.toString().split("\t");

        double dist = 0;
        double[] point = new double[feature.length-3];
        for(int j = 3; j < feature.length; j++){
            point[j-3] = Double.parseDouble(feature[j].trim());
        }

        int len = centroidsList.length;
        double[] diffList = new double[len];
        double minDiff = Double.MAX_VALUE;
        int minIndex = -1;
        for(int i =0; i < len; i++){
            try {
                diffList[i] = calcDist(centroidsList[i], point);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
            if(diffList[i] < minDiff){
                minDiff = diffList[i];
                minIndex = i;
            }
        }
        //DoubleWritable minDiffDW = new DoubleWritable(minDiff);
        Text keyWithCentroid = new Text("" + minIndex);
        
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < point.length; i++){
            sb.append(point[i] + "\t");
        }
        String pointStr = sb.toString().trim();
        
        
        Text pointText = new Text(1 + "#" + pointStr);
        context.write(keyWithCentroid, pointText);
    }
}