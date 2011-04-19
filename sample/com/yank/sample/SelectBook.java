/**
 * Copyright 2011 Xeiam LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yank.sample;

import java.util.Properties;

import com.xeiam.yank.DBConnectionManager;
import com.xeiam.yank.PropertiesUtils;

/**
 * Selects a single Book Bean from the BOOKS table. Demonstrates using a SQL Key in SQL.properties
 * 
 * @author timmolter
 */
public class SelectBook {

    public static void main(String[] args) {

        Properties dbprops = PropertiesUtils.getPropertiesFromClasspath("DB.properties");
        Properties sqlprops = PropertiesUtils.getPropertiesFromClasspath("SQL.properties");

        DBConnectionManager.INSTANCE.init(dbprops, sqlprops);

        Book book = BooksDAO.selectBook("Cryptonomicon");
        System.out.println(book.toString());

        DBConnectionManager.INSTANCE.release();

    }
}
