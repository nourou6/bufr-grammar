/*
MIT License

Copyright (c) 2019 Markus Heene <Markus.Heene@dwd.de>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Collections;

import java.util.logging.Logger;

public class CountReplicationDescriptor extends BUFRBaseListener {

    private static Logger log = Logger.getLogger(CountReplicationDescriptor.class.getName());

    private int counterDescriptors = 0;
    private int numberOfFixedReplications = 0;
    private int numberOfDelayedReplications = 0;

    private ArrayList<ReplicationDescriptor> descriptorList = new ArrayList<>();
    private List<String> errors = new ArrayList<>();

    private void addFixedReplicationDescriptorToList(String name, int replications) {
        Iterator<ReplicationDescriptor> it = descriptorList.iterator();
        while (it.hasNext()) {
            ReplicationDescriptor rd = it.next();
            rd.reduceReplications();
            if (rd.getReplications() == 0) {
                it.remove();
            }
        }
        int size = descriptorList.size();
        ReplicationDescriptor p_descriptor = new ReplicationDescriptor(size, name, "fixed", replications);
        descriptorList.add(p_descriptor);
    }

    private void addDelayedReplicationDescriptorToList(String name, int replications) {

        Iterator<ReplicationDescriptor> it = descriptorList.iterator();
        while (it.hasNext()) {
            ReplicationDescriptor rd = it.next();
            rd.reduceReplications(2);
            if (rd.getReplications() == 0) {
                it.remove();
            }
        }

        int size = descriptorList.size();
        ReplicationDescriptor p_descriptor = new ReplicationDescriptor(size, name, "delayed", replications);
        descriptorList.add(p_descriptor);
    }

    private void processDescriptor() {
        Iterator<ReplicationDescriptor> it = descriptorList.iterator();

        while (it.hasNext()) {
            ReplicationDescriptor rd = it.next();
            rd.reduceReplications();
            if (rd.getReplications() == 0) {
                it.remove();
            }
        }
    }

    @Override
    public void enterFixed_replication_descriptor(BUFRParser.Fixed_replication_descriptorContext ctx) {
        String fixReplication = ctx.getText();
        log.finest("Fixed Replication Ctx: " + fixReplication);
        String[] parts = fixReplication.split(" ");
        
        log.finest("Replications: " + Integer.parseInt(parts[1]));
        numberOfFixedReplications = Integer.parseInt(parts[1]);
        addFixedReplicationDescriptorToList(fixReplication, numberOfFixedReplications);
	counterDescriptors++;
        
    }

    @Override
    public void enterElement_descriptor(BUFRParser.Element_descriptorContext ctx) {
        log.finest("Element Descriptor Ctx: " + ctx.getText());
        counterDescriptors++;

        if (numberOfFixedReplications > 0) {
            numberOfFixedReplications--;
        }

        if (numberOfDelayedReplications > 0) {
            numberOfDelayedReplications--;
        }

        this.processDescriptor();
    }

    
    @Override
    public void enterOperator_descriptor(BUFRParser.Operator_descriptorContext ctx) {
        log.finest("Operator Descriptor Ctx: " + ctx.getText());
        counterDescriptors++;
        this.processDescriptor();
    }

    @Override
    public void enterOperator_236000(BUFRParser.Operator_236000Context ctx) {
        log.finest("Operator 236000 Descriptor Ctx: " + ctx.getText());
	// Simple check to detect operator 2 36 000 at the beginning of a template
	if (counterDescriptors == 0) {
	    errors.add("Operator 236000 at the beginning of template is not allowed");
	}
        counterDescriptors++;
        this.processDescriptor();
    }

    @Override
    public void enterAssociated_field_significance(BUFRParser.Associated_field_significanceContext ctx) {
        log.finest("Associated Field Significance  Ctx: " + ctx.getText());
        counterDescriptors++;
        this.processDescriptor();
    }

    @Override
    public void enterData_present_indicator(BUFRParser.Data_present_indicatorContext ctx) {
        log.finest("Data Present Indicator Ctx: " + ctx.getText());
        counterDescriptors++;
        this.processDescriptor();

    }

    
    @Override
    public void enterSequence_descriptor(BUFRParser.Sequence_descriptorContext ctx) {
        log.finest("Sequence Descriptor Ctx: " + ctx.getText());
        counterDescriptors++;
        this.processDescriptor();
    }

    @Override
    public void enterReplication_descriptor(BUFRParser.Replication_descriptorContext ctx) {
        log.finest("Replication Ctx: " + ctx.getText());
    }

   
    @Override public void enterDelayed_replication_expr_part(BUFRParser.Delayed_replication_expr_partContext ctx) {
        String delayedReplication = ctx.getText();
        log.finest("Delayed Replication Ctx: " + delayedReplication);
        String[] parts = delayedReplication.split(" ");
        // log.finest("" + parts.length + " x:" + parts[1]);
        log.finest("Replications: " + Integer.parseInt(parts[1]));
        numberOfDelayedReplications = Integer.parseInt(parts[1]);
        addDelayedReplicationDescriptorToList(delayedReplication, numberOfDelayedReplications);
	counterDescriptors++;
     }

    @Override public void enterDelayed_replication_expr_one_element(BUFRParser.Delayed_replication_expr_one_elementContext ctx) { 
        String delayedReplication = ctx.getText();
        log.finest("Delayed Replication Ctx: " + delayedReplication);
        String[] parts = delayedReplication.split(" ");
        // log.finest("" + parts.length + " x:" + parts[1]);
        log.finest("Replications: " + Integer.parseInt(parts[1]));
        numberOfDelayedReplications = Integer.parseInt(parts[1]);
        addDelayedReplicationDescriptorToList(delayedReplication, numberOfDelayedReplications);
	counterDescriptors++;
    }
    @Override
    public void exitTemplate(BUFRParser.TemplateContext ctx) {
        log.finest("Number of FixedReplications: " + numberOfFixedReplications);
        log.finest("Number of DelayedReplications: " + numberOfDelayedReplications);
        log.finest("Counter of Descriptors: " + counterDescriptors);
        if (!descriptorList.isEmpty()) {
            errors.add("Error in Replications detected.");
            errors.add("DescriptorList:");  
            Iterator<ReplicationDescriptor> it = descriptorList.iterator();
            while (it.hasNext()) {
                errors.add(it.next().toString());
            }
        }
    }

    public List<String> getErrors(){
        return Collections.unmodifiableList(errors);
    }

  

    class ReplicationDescriptor {
        private int position;
        private int replications;
        private String type;
        private String name;

        public ReplicationDescriptor(int position, String name, String type, int replications) {
            this.position = position;
            this.replications = replications;
            this.name = name;
            this.type = type;
        }

        public int getReplications() {
            return this.replications;
        }

        public int getPosition() {
            return this.position;
        }

        public String getType() {
            return this.type;
        }

        public String getName() {
            return this.name;
        }

        public void reduceReplications(int count) {
            this.replications = this.replications - count;
        }

        public void reduceReplications() {
            reduceReplications(1);
        }

        @Override
        public String toString() {
            return "Descriptor: Position " + this.position + " Name: " + this.name + " Replications: "
                    + this.replications;
        }
    }
}
