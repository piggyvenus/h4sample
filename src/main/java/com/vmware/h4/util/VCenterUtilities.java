package com.vmware.h4.util;

import java.util.ArrayList;
import java.util.List;


import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.TraversalSpec;

public class VCenterUtilities {
	
	public VCenterUtilities() {
		
	}
	public List<ObjectContent> findAllObjects(
            com.vmware.vim25.VimPortType vimPort, com.vmware.vim25.ServiceContent serviceContent,
            String objectType, String... properties) throws Exception {

        // Get references to the ViewManager and PropertyCollector
        com.vmware.vim25.ManagedObjectReference viewMgrRef = serviceContent.getViewManager();
        ManagedObjectReference propColl = serviceContent.getPropertyCollector();

        // use a container view for virtual machines to define the traversal
        // - invoke the VimPortType method createContainerView (corresponds
        // to the ViewManager method) - pass the ViewManager MOR and
        // the other parameters required for the method invocation
        // (use a List<String> for the type parameter's string[])
        List<String> typeList = new ArrayList<String>();
        typeList.add(objectType);

        ManagedObjectReference cViewRef = vimPort.createContainerView(viewMgrRef,
                serviceContent.getRootFolder(), typeList, true);

        // create an object spec to define the beginning of the traversal;
        // container view is the root object for this traversal
        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(cViewRef);
        oSpec.setSkip(true);

        // create a traversal spec to select all objects in the view
        com.vmware.vim25.TraversalSpec tSpec = new TraversalSpec();
        tSpec.setName("traverseEntities");
        tSpec.setPath("view");
        tSpec.setSkip(false);
        tSpec.setType("ContainerView");

        // add the traversal spec to the object spec;
        // the accessor method (getSelectSet) returns a reference
        // to the mapped XML representation of the list; using this
        // reference to add the spec will update the selectSet list
        oSpec.getSelectSet().add(tSpec);

        // specify the properties for retrieval
        // (virtual machine name, network summary accessible, rp runtime props);
        // the accessor method (getPathSet) returns a reference to the mapped
        // XML representation of the list; using this reference to add the
        // property names will update the pathSet list
        PropertySpec pSpec = new PropertySpec();
        pSpec.setType(objectType);
        if (properties != null) {
            for (String property : properties) {
                pSpec.getPathSet().add(property);
            }
        }

        // create a PropertyFilterSpec and add the object and
        // property specs to it; use the getter methods to reference
        // the mapped XML representation of the lists and add the specs
        // directly to the objectSet and propSet lists
        PropertyFilterSpec fSpec = new PropertyFilterSpec();
        fSpec.getObjectSet().add(oSpec);
        fSpec.getPropSet().add(pSpec);

        // Create a list for the filters and add the spec to it
        List<PropertyFilterSpec> fSpecList = new ArrayList<PropertyFilterSpec>();
        fSpecList.add(fSpec);

        // get the data from the server
        RetrieveOptions retrieveOptions = new RetrieveOptions();
        RetrieveResult props = vimPort.retrievePropertiesEx(propColl, fSpecList,
                retrieveOptions);

        // go through the returned list and print out the data
        if (props != null) {
            return props.getObjects();
        }
        return null;
	}
}
