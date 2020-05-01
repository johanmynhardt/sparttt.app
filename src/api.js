'use strict';

const AWS = require('aws-sdk');
const s3 = new AWS.S3();

const config = {
  bucket: 'spartan-harriers',
  context:'timetrial-serverless'
};

module.exports.eventUpload = (event, context, callback) => {

  //console.info('received event:', JSON.stringify(event));

  const filename = event.headers['x-filename'];
  const eventId = event.pathParameters.eventId;
  const data = event.body;
  const key = `${config.context}/${eventId}/${filename}`;

  s3.putObject({
    Bucket: config.bucket,
    Key: key,
    Body: data
  }, (err, data) => {
    if (err) {
      console.error("Upload failed", err);
      callback(err);
    } else {
      callback(null, {
        statusCode: 200,
        body: JSON.stringify({
          message: `Uploaded to ${key}`
        })
      });
    }
  });
}


const fetchFile = f => {
    return new Promise((resolve, reject) => {
        s3.getObject({Bucket: config.bucket, Key: f}, function(err, data) {
            if (err) {
                reject(err);
            } else {
                resolve({
                    filename: f.split('/').reduce((acc, next) => next),
                    content: data.Body.toString()
                });
            }
        });
    });
};

module.exports.forConsolidation = async (event) => {
    // console.info('got event: ', event);
    
    let bucketParams = {
        Bucket: config.bucket,
        Prefix: `${config.context}/${event.pathParameters.eventId}`
    };
    
    // console.info('listing bucket: ', bucketParams);
    
    let fl = [];
    
    let fileList = await s3.listObjectsV2(bucketParams).promise();
    fileList.Contents.filter(f => f.Key.endsWith('.csv')).forEach(function(f) {
        fl.push(fetchFile(f.Key));
    }); 
    
    let body = {
        files: (await Promise.all(fl))
    };
    const response = {
        statusCode: 200,
        headers: {
          'content-type': 'application/json',
          'Access-Control-Allow-Origin': '*'
        },
        body: JSON.stringify(body),
    };
    return response;
};
