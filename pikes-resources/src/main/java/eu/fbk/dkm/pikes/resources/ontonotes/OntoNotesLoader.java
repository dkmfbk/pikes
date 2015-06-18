package eu.fbk.dkm.pikes.resources.ontonotes;

import eu.fbk.dkm.pikes.resources.ontonotes.frames.Frameset;
import eu.fbk.dkm.pikes.resources.ontonotes.frames.Note;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * Created by alessio on 07/05/15.
 */

public class OntoNotesLoader {

	public static <T> T load(Class<T> thisClass, File file) throws SAXException, ParserConfigurationException, FileNotFoundException, JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(thisClass);

		SAXParserFactory spf = SAXParserFactory.newInstance();

		spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		spf.setFeature("http://xml.org/sax/features/validation", false);

		XMLReader xmlReader = spf.newSAXParser().getXMLReader();
		InputSource inputSource = new InputSource(new FileReader(file));
		SAXSource source = new SAXSource(xmlReader, inputSource);

		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

		return thisClass.cast(jaxbUnmarshaller.unmarshal(source));
	}

	public static void main(String[] args) {
		File file = new File("/Users/alessio/Desktop/eu.fbk.dkm.pikes.resources.ontonotes-release-5.0/data/files/data/english/metadata/frames/look-v.xml");
		try {
			Frameset frameset = load(Frameset.class, file);
			for (Object noteOrPredicate : frameset.getNoteOrPredicate()) {
				if (noteOrPredicate instanceof Note) {
					Note note = (Note) noteOrPredicate;
					System.out.println(note.getvalue());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
