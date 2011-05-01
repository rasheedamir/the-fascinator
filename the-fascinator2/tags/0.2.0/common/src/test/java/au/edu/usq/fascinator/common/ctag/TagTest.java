package au.edu.usq.fascinator.common.ctag;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Syntax;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.URI;

public class TagTest {

    private Model model;

    private URI about;

    @Before
    public void setup() {
        model = RDF2Go.getModelFactory().createModel();
        model.open();
        about = model.createURI("file://pictures/lolcats/1.jpg");
    }

    public void createTaggedContent() throws Exception {
        Tag tag1 = new Tag(model, "urn:1", true);
        tag1.setMeans(model
                .createURI("http://fascinator.usq.edu.au/tags/lolcat"));
        tag1.setTaglabel(model.createPlainLiteral("lolcat"));

        // other tag
        Tag tag2 = new Tag(model, "urn:2", true);
        tag2.setMeans(model
                .createURI("http://fascinator.usq.edu.au/tags/other"));
        tag2.setTaglabel(model.createPlainLiteral("other"));

        TaggedContent content = new TaggedContent(model, about, true);
        content.addTagged(tag1);
        content.addTagged(tag2);

        System.out.println(model.serialize(Syntax.RdfXml));
        model.close();
    }

    @Test
    public void readTaggedContent() throws Exception {
        model.readFrom(getClass().getResourceAsStream("/tags.rdf"));
        TaggedContent[] contents = TaggedContent.getAllInstances_as(model)
                .asArray();
        for (TaggedContent content : contents) {
            System.out.println("content: " + content);
            Tag[] tags = content.getAllTagged_as().asArray();
            Assert.assertEquals(2, tags.length);
            for (Tag tag : tags) {
                System.out.println("tag: " + tag);
                Node[] meansNodes = tag.getAllMeans_asNode_().asArray();
                for (Node means : meansNodes) {
                    System.out.println("means: " + means);
                }
                Node[] labels = tag.getAllTaglabel_asNode_().asArray();
                for (Node label : labels) {
                    System.out.println("label: " + label);
                    Assert.assertTrue("lolcat".equals(label.toString())
                            || "other".equals(label.toString()));
                }
            }
        }
        model.close();
    }
}
