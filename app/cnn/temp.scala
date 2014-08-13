var ids=ArrayBuffer[Long]()
for(var_id <- "{1,2,3,4}".toString.split(",|\\}|\\{") if var_id!="")
	ids+=var_id.toLong
for( a <- 0 to ids.length-1)
    println(ids(a))
