#!/bin/bash    
  
# Define the version number as a variable    
VERSION="1.12.2"    
  
# Define the source and target directories    
SOURCE_DIR="./${VERSION}LiteLoader/"    
TMP_DIR="${SOURCE_DIR}.tmp/"    
# We'll create the JAR files in the temp directory first    
TEMP_JAR_MCFORGE="fci.jar"    
TEMP_JAR_LITELOADER="liteloaderfci.jar"  
  
# Make sure the source directory exists    
if [ ! -d "$SOURCE_DIR" ]; then    
    echo "Error: Source directory $SOURCE_DIR does not exist."    
    exit 1    
fi    
  
# Function to handle the remapping and JAR creation process  
remap_and_jar() {  
    local input_jar=$1  
    local output_jar=$2  
    local srg_file="featurecreep-intermediary-${VERSION}.pdme"  
  
    # Run the java command with the AssistRemapper  
    java -jar AssistRemapper-1.1.jar "${input_jar}" "${srg_file}" "${TMP_DIR}"  
  
    # Check if the java command was successful  
    if [ $? -ne 0 ]; then    
        echo "Error: The java command failed for $input_jar."    
        exit 1    
    fi    
  
    # Zip the contents of the temp directory into the JAR file  
    cd "${TMP_DIR}" || exit  
    zip -r "${output_jar}" *  

    # Check if the JAR file was created successfully  
    if [ ! -f "$output_jar" ]; then    
        echo "Error: JAR file $output_jar was not created."    
        exit 1    
    fi 

    cd - > /dev/null # Return to the original directory  
  
 
}  
  
# Process the MCFORGE JAR  
remap_and_jar "${SOURCE_DIR}unmapped.jar" "${TEMP_JAR_MCFORGE}"  
  
# Copy the JAR file from the temp directory to the source directory  
cp "${TMP_DIR}${TEMP_JAR_MCFORGE}" "${SOURCE_DIR}"  
  
  # Delete the temp directory  
rm -rf "${TMP_DIR}"  
  
# Process the LITELOADER JAR (assuming it's called 'unmapped-liteloader.jar' in the source dir)  
remap_and_jar "${SOURCE_DIR}liteloaderunmapped.jar" "${TEMP_JAR_LITELOADER}"  
  
# Copy the LITELOADER JAR file from the temp directory to the source directory  
cp "${TMP_DIR}${TEMP_JAR_LITELOADER}" "${SOURCE_DIR}"  
  
# Delete the temp directory  
rm -rf "${TMP_DIR}"  
  
# Now you can run the cfr command against the JARs in the source directory  
cfr "${SOURCE_DIR}fci.jar" --outputpath "${SOURCE_DIR}src" --clobber true  
#cfr "${SOURCE_DIR}liteloaderfci.jar" --outputpath "${SOURCE_DIR}src_liteloader" --clobber true  
  
# Check if the cfr commands were successful  
if [ $? -ne 0 ]; then    
    echo "Error: One of the cfr commands failed."    
    exit 1    
fi    
  
echo "Done!"
