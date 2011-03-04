package com.bacaj.musictoS3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import android.util.Log;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class S3 {

	private static AmazonS3 mS3 = null;
	private static ObjectListing objListing = null;
	public static final String BUCKET_NAME = "_bucket_name";
	public static final String OBJECT_NAME = "_object_name";
	public static BasicAWSCredentials mCredentials;
	private static String TAG_NAME = "S3";

	static {
		System.setProperty("org.xml.sax.driver","org.xmlpull.v1.sax2.Driver");
		try {
			XMLReader reader = XMLReaderFactory.createXMLReader();
		}
		catch ( SAXException e ) {
			Log.e( "SAXException", e.getMessage() );
		}
	}

	public static AmazonS3 getInstance() {
        if ( mS3 == null && mCredentials != null) {
		    mS3 = new AmazonS3Client( mCredentials );
        }

        return mS3;
	}

	public static boolean loadCredentials(InputStream aInStream) throws IOException{

		Properties properties = new Properties();
        properties.load( aInStream);

        String accessKeyId = properties.getProperty( "accessKey" );
        String secretKey = properties.getProperty( "secretKey" );

        if ( ( accessKeyId == null ) || ( accessKeyId.equals( "" ) ) ||
        	 ( accessKeyId.equals( "CHANGEME" ) ) ||( secretKey == null )   ||
             ( secretKey.equals( "" ) ) || ( secretKey.equals( "CHANGEME" ) ) ) {
            Log.e( "AWS", "Aws Credentials not configured correctly." );
        } else {
        	mCredentials = new BasicAWSCredentials( properties.getProperty( "accessKey" ), properties.getProperty( "secretKey" ) );
        }
        return mCredentials != null;

	}

	public static List<String> getBucketNames() {
		List buckets = getInstance().listBuckets();

		List<String> bucketNames = new ArrayList<String>( buckets.size() );
		Iterator<Bucket> bIter = buckets.iterator();
		while(bIter.hasNext()){
			bucketNames.add((bIter.next().getName()));
		}
		return bucketNames;
	}

	public static List<String> getObjectNamesForBucket( String bucketName ) {
		ObjectListing objects = getInstance().listObjects( bucketName );
		objListing = objects;
		List<String> objectNames = new ArrayList<String>( objects.getObjectSummaries().size() );
		Iterator<S3ObjectSummary> oIter = objects.getObjectSummaries().iterator();
		while(oIter.hasNext()){
			objectNames.add(oIter.next().getKey());
		}
		return objectNames;
	}

	public static List<String> getObjectNamesForBucket( String bucketName , int numItems) {
		ListObjectsRequest req= new ListObjectsRequest();
		req.setMaxKeys(new Integer(numItems));
		req.setBucketName(bucketName);
		ObjectListing objects = getInstance().listObjects( req );
		objListing = objects;
		List<String> objectNames = new ArrayList<String>( objects.getObjectSummaries().size());
		Iterator<S3ObjectSummary> oIter = objects.getObjectSummaries().iterator();
		while(oIter.hasNext()){
			objectNames.add(oIter.next().getKey());
		}

		return objectNames;
	}

	public static List<String> getMoreObjectNamesForBucket() {
		try{
			ObjectListing objects = getInstance().listNextBatchOfObjects(objListing);
			objListing = objects;
			List<String> objectNames = new ArrayList<String>( objects.getObjectSummaries().size());
			Iterator<S3ObjectSummary> oIter = objects.getObjectSummaries().iterator();
			while(oIter.hasNext()){
				objectNames.add(oIter.next().getKey());
			}
			return objectNames;
		} catch (NullPointerException e){
			return new ArrayList<String>();
		}

	}
	public static void createBucket( String bucketName ) {
		getInstance().createBucket( bucketName );
	}

	public static void deleteBucket( String bucketName ) {
		getInstance().deleteBucket(  bucketName );
	}

	public static void createObjectForBucket( String bucketName, String objectName, String data ) {
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream( data.getBytes() );
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength( data.getBytes().length );
			getInstance().putObject(bucketName, objectName, bais, metadata );
		}
		catch ( Exception exception ) {
			Log.e(TAG_NAME, "createObjectForBucket" );
		}
	}

	/**
	 * Method uploads a file to a bucket on S3 cloud
	 * @param aBucketName - The name of the bucket to upload to
	 * @param aObjectName - The name of the object on the cloud
	 * @param aFile       - The local filename with path for reading contents
	 */
	public static void createObjectForBucket(String aBucketName, String aObjectName, File aFile){


		FileInputStream lInputStream;
		int lLength = 0;
		byte lFileContent[] = null;

		try {
			// create a filestream and read the contents into a byte array
			lInputStream = new FileInputStream(aFile);
			lLength = (int)aFile.length();
			lFileContent = new byte[lLength];
			lInputStream.read(lFileContent);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			if(lFileContent != null){

				// create MetaData and put the object to S3
				ByteArrayInputStream lBAIS = new ByteArrayInputStream( lFileContent );
				ObjectMetadata lMetadata = new ObjectMetadata();
				lMetadata.setContentLength( lLength );
				getInstance().putObject(aBucketName, aObjectName, lBAIS, lMetadata );
			}
		}
		catch ( Exception exception ) {
			Log.e( TAG_NAME, "Unable to putObject, Reason:" + exception.getMessage() );
		}

	}

	public static void deleteObject( String bucketName, String objectName ) {
		getInstance().deleteObject( bucketName, objectName );
	}

	public static String getDataForObject( String bucketName, String objectName ) {
		return read( getInstance().getObject( bucketName, objectName ).getObjectContent() );
	}

	protected static String read( InputStream stream ) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream( 8196 );
			byte[] buffer = new byte[1024];
			int length = 0;
			while ( ( length = stream.read( buffer ) ) > 0 ) {
				baos.write( buffer, 0, length );
			}

			return baos.toString();
		}
		catch ( Exception exception ) {
			return exception.getMessage();
		}
	}
}
