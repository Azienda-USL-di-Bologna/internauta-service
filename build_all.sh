#!/bin/bash
##########################################################
# Script di build dei progetti Maven                     
#                                                        
# Questo script automatizza il processo di build per     
# diversi moduli di progetto utilizzando Maven.         
# L'utente puo scegliere se eseguire o saltare la build  
# per ciascun modulo.      
#
# Autore: Giuseppe Russo <g.russo@dilaxia.com>
# Data di creazione: 01/06/2023
# Versione: 1.0                              
##########################################################

# Impostazioni di Maven
export M2_HOME=C:/Software/maven
export PATH=$M2_HOME/bin:$PATH

# Build dei moduli
cd submodules/next-spring-data-rest-framework
echo ""
read -t 1 -p "Build next-spring-data-rest-framework? [Y/N]: " -n 1 build_nsdrf
build_nsdrf=${build_nsdrf:-Y}
echo ""
if [[ $build_nsdrf == "Y" || $build_nsdrf == "y" ]]; then
    mvn clean install -DskipTests
else
    echo "Skipping next-spring-data-rest-framework build..."
fi

cd ../jenesis-projections-generator
echo ""
read -t 1 -p "Build jenesis-projections-generator? [Y/N]: " -n 1 build_jpg
build_jpg=${build_jpg:-Y}
echo ""
if [[ $build_jpg == "Y" || $build_jpg == "y" ]]; then
    mvn clean install
else
    echo "Skipping jenesis-projections-generator build..."
fi

cd ../internauta-utils
echo ""
read -t 1 -p "Build internauta-utils jpa-tools? [Y/N]: " -n 1 build_iu
build_iu=${build_iu:-Y}
echo ""
if [[ $build_iu == "Y" || $build_iu == "y" ]]; then
    mvn --projects jpa-tools --also-make clean install
else
    echo "Skipping internauta-utils jpa-tools build..."
fi

cd ../internauta-model
echo ""
read -t 1 -p "Build internauta-model? [Y/N]: " -n 1 build_im
build_im=${build_im:-Y}
echo ""
if [[ $build_im == "Y" || $build_im == "y" ]]; then
    mvn clean install
else
    echo "Skipping internauta-model build..."
fi

cd ../internauta-utils
echo ""
read -t 1 -p "Build internauta-utils again? [Y/N]: " -n 1 build_iu_again
build_iu_again=${build_iu_again:-Y}
echo ""
if [[ $build_iu_again == "Y" || $build_iu_again == "y" ]]; then
    mvn clean install -DskipTests
else
    echo "Skipping internauta-utils build..."
fi

cd ../blackbox-permessi
echo ""
read -t 1 -p "Build blackbox-permessi? [Y/N]: " -n 1 build_bp
build_bp=${build_bp:-Y}
echo ""
if [[ $build_bp == "Y" || $build_bp == "y" ]]; then
    mvn clean install
else
    echo "Skipping blackbox-permessi build..."
fi

# Build internauta service
cd ../..
echo ""
read -t 1 -p "Build internauta service? [Y/N]: " -n 1 build_parent
build_parent=${build_parent:-Y}
echo ""
if [[ $build_parent == "Y" || $build_parent == "y" ]]; then
    mvn clean install -DskipTests
else
    echo "Skipping internauta service build..."
fi

echo -e "\nPremi Invio per continuare..."
read
echo "Build Complete."