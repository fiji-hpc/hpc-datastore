package cz.it4i.fiji.datastore.bdv_server;

import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import cz.it4i.fiji.datastore.register_service.Dataset;
import cz.it4i.fiji.datastore.register_service.DatasetRepository;
import cz.it4i.fiji.datastore.register_service.DatasetVersion;

/**
 * Provides a list of available datasets on this {@link BigDataServer}
 *
 * @author HongKee Moon &lt;moon@mpi-cbg.de&gt; adapted by Jan Kožusznik
 *         &lt;jan.kozusznik@vsb.cz&gt;
 */
@SuppressWarnings("javadoc")
@ApplicationScoped
class JsonDatasetListHandlerTS 
{

	@Inject
	DatasetRepository datasetRepository;

	public void run(UUID uuid, final HttpServletResponse response,
		URI baseURI)
		throws IOException
	{
		run(uuid, response, baseURI, false);
	}

	public void run(UUID uuid, final HttpServletResponse response, URI baseURI,
		boolean allVersionsInOne) throws IOException
	{
		list(uuid, response, baseURI, allVersionsInOne);
	}

	private void list(UUID uuid, final HttpServletResponse response,
		URI baseURI, boolean allVersionsInOne)
		throws IOException
	{
		Dataset dataset = datasetRepository.findByUUID(uuid);

		response.setContentType( "application/json" );
		response.setStatus( HttpServletResponse.SC_OK );

		try (final PrintWriter ow = response.getWriter()) {
			getJsonDatasetList(dataset, ow, baseURI, allVersionsInOne);
		}
	}

	private void getJsonDatasetList(Dataset dataset, final PrintWriter out,
		URI baseURI, boolean allVersionsInOne)
		throws IOException
	{
		try (final JsonWriter writer = new JsonWriter(out)) {

			writer.setIndent("\t");

			writer.beginObject();

			getContexts(dataset, writer, baseURI, allVersionsInOne);

			writer.endObject();

			writer.flush();

		}
	}

	private String getContexts(Dataset dataset, final JsonWriter writer,
		URI baseURI, boolean allVersionsInOne)
		throws IOException
	{
		final StringBuilder sb = new StringBuilder();
		if (!allVersionsInOne) {
			for (final DatasetVersion datasetVersion : dataset.getDatasetVersion()) {

				writeInfoAboutVersion(dataset, writer, baseURI, Integer.toString(
					datasetVersion.getValue()));

			}
			if (!dataset.getDatasetVersion().isEmpty()) {
				writeInfoAboutVersion(dataset, writer, baseURI, "mixedLatest");
			}
		}
		else {
			if (!dataset.getDatasetVersion().isEmpty()) {
				writeInfoAboutVersion(dataset, writer, baseURI, "all");
			}
		}
		return sb.toString();
	}

	public void writeInfoAboutVersion(Dataset dataset, final JsonWriter writer,
		URI baseURI, String version) throws IOException
	{
		final String datasetName = "dataset:" + dataset.getUuid() + ", version:" +
			version;

		writer.name(datasetName).beginObject();

		writer.name("id").value(datasetName);

		// writer.name( "desc" ).value( contextHandler.getDescription() );
		writer.name("description").value("NotImplemented");
		boolean endsWithSlash = baseURI.toString().endsWith("/");
		writer.name("datasetUrl").value(baseURI.resolve((endsWithSlash ? "../"
			: "./") + version + "/")
			.toString());

		writer.endObject();
	}
}
