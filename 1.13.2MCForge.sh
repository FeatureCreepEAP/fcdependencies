#!/bin/bash  
  
# Define the version number as a variable  
VERSION="1.13.2"  
  
# Define the source and target directories  
SOURCE_DIR="./${VERSION}MCForge/"  
TMP_DIR="${SOURCE_DIR}.tmp/"  
# We'll create the JAR file in the temp directory first  
TEMP_JAR="${TMP_DIR}fci.jar"  
  
# Make sure the source directory exists  
if [ ! -d "$SOURCE_DIR" ]; then  
    echo "Error: Source directory $SOURCE_DIR does not exist."  
    exit 1  
fi  
  
# Run the java command with the AssistRemapper  
java -jar AssistRemapper-1.1.jar "${SOURCE_DIR}unmapped.jar" "featurecreep-intermediary-${VERSION}-srg.pdme" "${TMP_DIR}"  
  
# Check if the java command was successful  
if [ $? -ne 0 ]; then  
    echo "Error: The java command failed."  
    exit 1  
fi  
  
# Zip the contents of the temp directory into fci.jar, excluding the temp directory itself  
cd "${TMP_DIR}" || exit  
zip -r "fci.jar" *  
cd - > /dev/null # Return to the original directory  
  
# Check if the JAR file was created successfully  
if [ ! -f "$TEMP_JAR" ]; then  
    echo "Error: JAR file $TEMP_JAR was not created."  
    exit 1  
fi  
  
# Copy the JAR file from the temp directory to the source directory  
cp "${TEMP_JAR}" "${SOURCE_DIR}"  
  
# Delete the temp directory  
rm -rf "${TMP_DIR}"  
  
# Now you can run the cfr command against the JAR in the source directory  
cfr "${SOURCE_DIR}fci.jar" --outputpath "${SOURCE_DIR}src" --clobber true  
  
# Check if the cfr command was successful  
if [ $? -ne 0 ]; then  
    echo "Error: The cfr command failed."  
    exit 1  
fi  
  
echo "Done!"
