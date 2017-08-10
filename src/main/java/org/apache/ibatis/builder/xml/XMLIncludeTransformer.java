/**
 *    Copyright 2009-2015 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.builder.xml;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.parsing.PropertyParser;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.session.Configuration;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Properties;

/**
 * @author Frank D. Martinez [mnesarco]
 */
@Slf4j
@ToString
public class XMLIncludeTransformer {

    private final Configuration configuration;
    private final MapperBuilderAssistant builderAssistant;

    public XMLIncludeTransformer(Configuration configuration, MapperBuilderAssistant builderAssistant) {
        log.debug("XMLIncludeTransformer({},{})", configuration, builderAssistant);
        this.configuration = configuration;
        this.builderAssistant = builderAssistant;
    }

    public void applyIncludes(Node source) {
        log.debug("applyIncludes({})", source);
        Properties variablesContext = new Properties();
        Properties configurationVariables = configuration.getVariables();
        if (configurationVariables != null) {
            variablesContext.putAll(configurationVariables);
        }
        applyIncludes(source, variablesContext);
    }

    /**
     * Recursively apply includes through all SQL fragments.
     *
     * @param source           Include node in DOM tree
     * @param variablesContext Current context for static variables with values
     */
    private void applyIncludes(Node source, final Properties variablesContext) {
        log.debug("applyIncludes({},{})", source, variablesContext);
        if (source.getNodeName().equals("include")) {
            // new full context for included SQL - contains inherited context and new variables from current include node
            Properties fullContext;

            String refid = getStringAttribute(source, "refid");
            // replace variables in include refid value
            refid = PropertyParser.parse(refid, variablesContext);
            Node toInclude = findSqlFragment(refid);
            Properties newVariablesContext = getVariablesContext(source, variablesContext);
            if (!newVariablesContext.isEmpty()) {
                // merge contexts
                fullContext = new Properties();
                fullContext.putAll(variablesContext);
                fullContext.putAll(newVariablesContext);
            } else {
                // no new context - use inherited fully
                fullContext = variablesContext;
            }
            applyIncludes(toInclude, fullContext);
            if (toInclude.getOwnerDocument() != source.getOwnerDocument()) {
                toInclude = source.getOwnerDocument().importNode(toInclude, true);
            }
            source.getParentNode().replaceChild(toInclude, source);
            while (toInclude.hasChildNodes()) {
                toInclude.getParentNode().insertBefore(toInclude.getFirstChild(), toInclude);
            }
            toInclude.getParentNode().removeChild(toInclude);
        } else if (source.getNodeType() == Node.ELEMENT_NODE) {
            NodeList children = source.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                applyIncludes(children.item(i), variablesContext);
            }
        } else if (source.getNodeType() == Node.ATTRIBUTE_NODE && !variablesContext.isEmpty()) {
            // replace variables in all attribute values
            source.setNodeValue(PropertyParser.parse(source.getNodeValue(), variablesContext));
        } else if (source.getNodeType() == Node.TEXT_NODE && !variablesContext.isEmpty()) {
            // replace variables ins all text nodes
            source.setNodeValue(PropertyParser.parse(source.getNodeValue(), variablesContext));
        }
    }

    private Node findSqlFragment(String refid) {
        log.debug("findSqlFragment({})", refid);
        refid = builderAssistant.applyCurrentNamespace(refid, true);
        try {
            XNode nodeToInclude = configuration.getSqlFragments().get(refid);
            return nodeToInclude.getNode().cloneNode(true);
        } catch (IllegalArgumentException e) {
            throw new IncompleteElementException("Could not find SQL statement to include with refid '" + refid + "'", e);
        }
    }

    private String getStringAttribute(Node node, String name) {
        log.debug("getStringAttribute({},{})", node, name);
        return node.getAttributes().getNamedItem(name).getNodeValue();
    }

    /**
     * Read placholders and their values from include node definition.
     *
     * @param node                      Include node instance
     * @param inheritedVariablesContext Current context used for replace variables in new variables values
     * @return variables context from include instance (no inherited values)
     */
    private Properties getVariablesContext(Node node, Properties inheritedVariablesContext) {
        log.debug("getVariablesContext({},{})", node, inheritedVariablesContext);
        Properties variablesContext = new Properties();
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                String name = getStringAttribute(n, "name");
                String value = getStringAttribute(n, "value");
                // Replace variables inside
                value = PropertyParser.parse(value, inheritedVariablesContext);
                // Push new value
                Object originalValue = variablesContext.put(name, value);
                if (originalValue != null) {
                    throw new BuilderException("Variable " + name + " defined twice in the same include definition");
                }
            }
        }
        return variablesContext;
    }

}
