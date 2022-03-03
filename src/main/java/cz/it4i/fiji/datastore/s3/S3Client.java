
package cz.it4i.fiji.datastore.s3;

import static java.util.stream.Collectors.toList;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.log4j.Log4j2;

@Log4j2
class S3Client {



	private final AmazonS3 client;

	private S3Settings settings;

	final static private String DELIMITER = "/";

	S3Client(S3Settings settings) {
		this.settings = settings;
		final AwsClientBuilder.EndpointConfiguration endpoint =
			new AwsClientBuilder.EndpointConfiguration(settings.getHostURL(), settings
				.getRegion());
		this.client = AmazonS3ClientBuilder.standard().withPathStyleAccessEnabled(
			true).disableChunkedEncoding().withEndpointConfiguration(endpoint)
			.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(
				settings.getAccessKey(), settings.getSecretKey()))).build();

		if (!client.doesBucketExistV2(settings.getBucket())) {
			client.createBucket(settings.getBucket());
		}
	}

	void copy(String src, String dst) {
		log.debug("copy: {} -> {}", src, dst);
		client.copyObject(settings.getBucket(), src, settings.getBucket(), dst);
	}

	void createFile(String resolve) {
		client.putObject(new PutObjectRequest(settings.getBucket(), resolve,
			new ByteArrayInputStream(new byte[0]), new ObjectMetadata()));
	}

	OutputStream getOutputStream(final String name) {
		Preconditions.checkNotNull(settings.getBucket());
		Preconditions.checkNotNull(name);
		final ByteArrayOutputStream result = new ByteArrayOutputStream() {
			@Override
			public void close() throws IOException {
				putObject(name, buf, count);
				super.close();
			}
		};
		return result;
	}

	InputStream getInputStream(final String name) {
		final S3Object object = this.client.getObject(settings.getBucket(), name);
		return object.getObjectContent();
	}

	boolean fileExists(final String name) {
		return this.client.doesObjectExist(settings.getBucket(), name);
	}

	boolean directoryExists(final String path) {
		final ListObjectsV2Request request = new ListObjectsV2Request()
			.withBucketName(settings.getBucket()).withPrefix(path.toString());

		return this.client.listObjectsV2(request).getKeyCount() > 0;
	}

	void deleteFile(final String name) {
		client.deleteObject(settings.getBucket(), name);
	}

	void deleteDirectory(final String name) {
		Preconditions.checkNotNull(name);


		final List<KeyVersion> toDelete = streamObjects(name).map(KeyVersion::new)
			.collect(toList());

		if (toDelete.isEmpty()) {
			log.info("Nothing to delete from S3 - bucket {}, file name {}", settings
				.getBucket(),
				name);
			return;
		}
		DeleteObjectsRequest req = new DeleteObjectsRequest(settings.getBucket())
			.withKeys(
			toDelete);
		DeleteObjectsResult response = client.deleteObjects(req);
		if (response.getDeletedObjects().size() < toDelete.size()) {
			final Set<String> deletedKeys = response.getDeletedObjects().stream().map(
				DeleteObjectsResult.DeletedObject::getKey).collect(Collectors.toSet());
			final Set<String> toDeleteKeys = toDelete.stream().map(
				KeyVersion::getKey).collect(Collectors.toSet());
			log.warn("Some files failed to delete - {}", Sets.difference(toDeleteKeys,
				deletedKeys));
		}
	}

	String getDelimiter() {
		return DELIMITER;
	}

	Stream<String> streamObjects(String prefix) {
		return this.client.listObjects(new ListObjectsRequest().withBucketName(
			settings.getBucket()).withPrefix(prefix)).getObjectSummaries()
			.stream().map(os -> os.getKey());
	}

	
	Stream<String> streamFolderObjects(String prefix) {
		return this.client.listObjects(new ListObjectsRequest().withBucketName(
			settings.getBucket()).withPrefix(prefix + "/").withDelimiter(DELIMITER))
			.getCommonPrefixes().stream();
	}

	Stream<String> getSubkeys(Stream<String> keys) {
		return keys.map(this::getFolderName);
	}

	private String getFolderName(String path) {
		if (!path.contains(DELIMITER)) {
			return path;
		}
		return path.substring(path.lastIndexOf(DELIMITER, path.length() - 2) + 1,
			path.length() - 1);
	}

	private void putObject(String name, byte[] data, int length) {
		ObjectMetadata obj = new ObjectMetadata();
		obj.setContentLength(length);
		S3Client.this.client.putObject(new PutObjectRequest(settings.getBucket(),
			name,
			new ByteArrayInputStream(data, 0, length), obj));
	}

}
