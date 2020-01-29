package org.apache.isis.subdomains.excel.fixtures.demoapp.todomodule.fixturescripts;

import org.apache.isis.subdomains.excel.fixtures.demoapp.todomodule.dom.ExcelDemoToDoItem;
import org.apache.isis.testing.fixtures.applib.teardown.TeardownFixtureAbstract;
import org.apache.isis.testing.fixtures.applib.legacy.teardown.TeardownFixtureAbstract2;

public class ExcelDemoToDoItem_tearDown extends TeardownFixtureAbstract {

    @Override
    protected void execute(final ExecutionContext executionContext) {
        deleteFrom(ExcelDemoToDoItem.class);
    }

}
